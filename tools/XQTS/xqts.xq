declare namespace xqts="http://exist-db.org/xquery/xqts";
declare namespace catalog="http://www.w3.org/2005/02/query-test-XQTSCatalog";

declare namespace props="java:java.util.Properties";

import module namespace util="http://exist-db.org/xquery/util";
import module namespace xdb="http://exist-db.org/xquery/xmldb";
import module namespace xdiff="http://exist-db.org/xquery/xmldiff";

(:~ ----------------------------------------------------------------------------------------
     W3C XQuery Test Suite
     
     This is the main module for running the XQTS on eXist. You can either
     run the whole test suite, a specific test group or a single test case.
     
     Setup:
     
     * Make sure the XmlDiff module is registered in conf.xml
     
     * Change the $xqts:XQTS_HOME variable below to point to the directory into
     which you unzipped the XQTS sources. 
     
     * Create a collection /db/XQTS in the database.
     
     * From the XQTS directory, upload XQTSCatalog.xml into the created
     collection.
     
     * Upload the "TestSources" directory so the source docs can be found
     in /db/XQTS/TestSources.
     
     * Run this script with the client.
     ------------------------------------------------------------------------------------------- :)
declare variable $xqts:XQTS_HOME { "file:///d:/Data/XQTS/" };

declare function xqts:create-collections($group as element(catalog:test-group)) as node() {
    let $rootColl := xdb:create-collection("/db/XQTS", "test-results")
    let $ancestors := reverse(($group/ancestor::catalog:test-group, $group))
    let $collection := xqts:create-collections($rootColl, $ancestors, "/db/XQTS/test-results")
    let $results := xdb:store($collection, "results.xml", <test-result failed="0" passed="0"/>)
    return
        doc($results)
};

declare function xqts:create-collections($parent as object,
    $pathElements as element()+, $currentPath as xs:string) 
as object {
    let $next := $pathElements[last()]
    let $remainder := subsequence($pathElements, 1, count($pathElements) - 1)
    let $newColl := xdb:create-collection($parent, $next/@name)
    return
        if ($remainder) then
            xqts:create-collections($newColl, $remainder, concat($currentPath, "/", $next/@name))
        else
            $newColl
};

declare function xqts:get-query($case as element(catalog:test-case)) {
   let $query-name := $case//catalog:query/@name
   let $path := concat( $xqts:XQTS_HOME, "Queries/XQuery/", $case/@FilePath, $query-name, ".xq" )
   let $xq-string := util:file-read($path)
   return $xq-string
};

declare function xqts:print-result($test-name as xs:string, $passed as xs:boolean, $query as xs:string, 
    $result as item()*, $expected as item()*, $case as element(catalog:test-case)) as element() {
    <test-case name="{$test-name}" result="{if ($passed) then 'pass' else 'fail'}">
    {
        if (not($passed)) then (
            <result>{$result}</result>,
            if ($expected instance of element() and count($expected/*) > 10) then
            	<expected truncated="">{$expected/*[position() < 10]}</expected>
            else
                <expected>{$expected}</expected>,
            (: element {$case/catalog:input-file/@variable} { xqts:getInputValue($case) }, :)
            <query>{$query}</query>
        ) else ()
    }
    </test-case>
};

declare function xqts:normalize-text($result as item()*) as xs:string {
    let $str := string-join($result, " ")
    return
        (: Remove leading and trailing whitespace :)
        replace(replace($str, "^\s+", ""), "\s+$", "")
};

declare function xqts:check-output($query as xs:string, $result as item()*, $case as element(catalog:test-case)) {
    let $output := $case/catalog:output-file[last()]
    return
        (: Comparison method: "Text" :)
        if ($output/@compare eq "Text") then
            let $text := util:file-read(concat($xqts:XQTS_HOME, "ExpectedTestResults/", $case/@FilePath,
                "/", $output/text()))
            let $test := xqts:normalize-text($text) eq xqts:normalize-text($result)
            return
                xqts:print-result($case/@name, $test, $query, $result, $text, $case)
        (: Comparison method: "XML" :)
        else if ($output/@compare eq "XML") then
            let $filePath := concat($xqts:XQTS_HOME, "ExpectedTestResults/", $case/@FilePath,
                $output/text())
            let $expected := doc(xdb:store("/db", "temp.xml", xs:anyURI($filePath), "text/xml"))
            let $test := xdiff:compare($expected, $result)
            return
                xqts:print-result($case/@name, $test, $query, $result, $expected, $case)
        (: Comparison method: "Fragment" :)
        else if ($output/@compare eq "Fragment") then
            let $filePath := concat($xqts:XQTS_HOME, "ExpectedTestResults/", $case/@FilePath, $output/text())
            let $expectedFrag := util:file-read($filePath)
            let $xmlFrag := concat("<f>", $expectedFrag, "</f>")
            let $log := util:log("DEBUG", ("Frag stored: ", $xmlFrag))
            let $expected := doc(xdb:store("/db", "temp.xml", $xmlFrag, "text/xml")) 
            let $test := xdiff:compare($expected, <f>{$result}</f>)
            return
                xqts:print-result($case/@name, $test, $query, $result, $expected, $case)
        (: Don't know how to compare :)
        else
            <error test="{$case/@name}">Unknown comparison method: {$output/@compare}.</error>
};

declare function xqts:run-test-case( $case as element(catalog:test-case)) as item()* {
   let $query := xqts:get-query($case)
   let $context :=
       <static-context>
       {
           for $input in $case/catalog:input-file
           return
               <variable name="{$input/@variable}">{xqts:get-input-value($input)}</variable>
       }
       </static-context>
   let $result := 
       util:catch("java.lang.Exception",
           let $result :=
               util:eval-with-context($query, $context, false())
           return
               xqts:check-output($query, $result, $case),
           if (exists($case/catalog:expected-error)) then
               <test-case name="{$case/@name}" result="pass"/>
           else
               <test-case name="{$case/@name}" result="fail">
                   <exception>{$util:exception-message}</exception>
                   <query>{$query}</query>
               </test-case>
       )
   return $result
};

declare function xqts:finish($result as element()) as empty() {
    let $passed := count($result//test-case[@result = 'pass'])
    let $failed := count($result//test-case[@result = 'fail'])
    return (
        update value $result/@passed with xs:int($passed),
        update value $result/@failed with xs:int($failed)
    )
};

declare function xqts:run-single-test-case($case as element(catalog:test-case),
    $resultRoot as element()?) as empty() {
    let $result := xqts:run-test-case($case)
    return
        update insert $result into $resultRoot
};

declare function xqts:run-test-group($group as element(catalog:test-group)) as empty() {
    (: Create the collection hierarchy for this group and get the results.xml doc to 
        append to. :)
    let $resultsDoc := xqts:create-collections($group)
    let $tests := $group/catalog:test-case
    return (
        (: Execute the test cases :)
        for $test in $tests
        let $log := util:log("DEBUG", ("Running test case: ", string($test/@name)))
        return
            xqts:run-single-test-case($test, $resultsDoc/test-result),
        if ($tests) then 
            xqts:finish($resultsDoc/test-result)
        else
            xdb:remove(util:collection-name($resultsDoc), util:document-name($resultsDoc)),
        (: Execute tests in child groups :)
        for $childGroup in $group/catalog:test-group
        let $log := util:log("DEBUG", ("Entering group: ", string($childGroup/@name)))
        return
            xqts:run-test-group($childGroup)
    )
};

declare function xqts:test-single($name as xs:string) as element() {
    let $test := //catalog:test-case[@name = $name]
    return
        if ($test) then
            let $resultsDoc := xqts:create-collections($test/parent::catalog:test-group)
            let $dummy := xqts:run-single-test-case($test, $resultsDoc/test-result)
            return
                $resultsDoc/test-result/test-case
        else
            util:log("WARN", ("Test case not found: ", $name))
};

declare function xqts:test-group($groupName as xs:string) as empty() {
    let $group := //catalog:test-group[@name = $groupName]
    return
        xqts:run-test-group($group)
};

declare function xqts:test-all() as empty() {
    for $test in /catalog:test-suite/catalog:test-group
    return
        xqts:test-group($test/@name)
};

declare function xqts:get-input-value($input as element(catalog:input-file)) as item()* {
   if ($input eq "emptydoc") then
       ()
   else (
       let $source := root($input)//catalog:source[@ID = $input/text()]
       return
           if (empty($source)) then
               concat("no input found: ", $input/text()) 
           else
               doc(concat("/db/XQTS/", $source/@FileName))
   )
};

(: xqts:test-single("Axes066-2") :)
xqts:test-group("PathExpr"),
collection("/db/XQTS")/test-result