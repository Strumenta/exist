# Failing Tests / Missing Functionality

## fn-transform-22

Succeeds when run as the only XQTS test, fails when run in "all the tests" mode. Should work, have UT-ed it.

## fn-transform-67

This shares the functionality of the `use-character-maps` serialization parameter of `fn:serialize`. This feature of `fn:serialize` has not been impllemented. We assume it is not trivial.

## fn-transform-70 to 79

These relate to the `requested-properties` parameter, which we have not implemented; this a group of somewhat unrelated configuration settings.

* For the `is-schema-aware` property, we can call `XsltCompiler.setSchemaAware()` on the Saxon compiler.

* Other properties, as used in the tests, we need to understand what they control and how to set/clear the values.

## fn-transform-901,902

These are `fn-transform-XSLT30=false` and `fn-transform-XSLT=false` and should not be run, IIRC ?


