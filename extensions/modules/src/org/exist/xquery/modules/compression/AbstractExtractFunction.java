package org.exist.xquery.modules.compression;

import java.io.ByteArrayInputStream;

import java.io.IOException;
import java.io.InputStream;

import org.exist.xquery.BasicFunction;
import org.exist.xquery.Cardinality;
import org.exist.xquery.FunctionCall;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.modules.ModuleUtils;
import org.exist.xquery.value.AnyURIValue;
import org.exist.xquery.value.Base64Binary;
import org.exist.xquery.value.BooleanValue;
import org.exist.xquery.value.FunctionReference;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.StringValue;
import org.exist.xquery.value.Type;

import org.exist.external.org.apache.commons.io.output.ByteArrayOutputStream;

import org.xml.sax.SAXException;

/**
 * @author Adam Retter <adam@exist-db.org>
 * @version 1.0
 */
public abstract class AbstractExtractFunction extends BasicFunction
{
    private FunctionCall entryFilterFunction = null;
    private FunctionCall entryDataFunction = null;
    private Sequence contextSequence;


    public AbstractExtractFunction(XQueryContext context, FunctionSignature signature)
    {
        super(context, signature);
    }

    @Override
    public Sequence eval(Sequence[] args, Sequence contextSequence) throws XPathException
    {
        this.contextSequence = contextSequence;

        if(args[0].isEmpty())
            return Sequence.EMPTY_SEQUENCE;

        //get the entry-filter function and check its types
        if(!(args[1] instanceof FunctionReference))
            throw new XPathException("No entry-filter function provided.");
        FunctionReference entryFilterFunctionRef = (FunctionReference)args[1].itemAt(0);
        entryFilterFunction = entryFilterFunctionRef.getFunctionCall();
        FunctionSignature entryFilterFunctionSig = entryFilterFunction.getSignature();
        if(entryFilterFunctionSig.getArgumentCount() < 2)
            throw new XPathException("entry-filter function must take at least 2 arguments.");
        SequenceType[] argTypes = entryFilterFunctionSig.getArgumentTypes();
        if(
            argTypes[0].getCardinality() != Cardinality.EXACTLY_ONE || !Type.subTypeOf(Type.ANY_URI, argTypes[0].getPrimaryType()) ||
            argTypes[1].getCardinality() != Cardinality.EXACTLY_ONE || !Type.subTypeOf(Type.STRING, argTypes[1].getPrimaryType()) ||
            !Type.subTypeOf(Type.BOOLEAN, entryFilterFunctionSig.getReturnType().getPrimaryType())
        ) throw new XPathException("entry-filter function does not match the expected function signature.");


        //get the entry-data function and check its types
        if(!(args[2] instanceof FunctionReference))
            throw new XPathException("No entry-data function provided.");
        FunctionReference entryDataFunctionRef = (FunctionReference)args[2].itemAt(0);
        entryDataFunction = entryDataFunctionRef.getFunctionCall();
        FunctionSignature entryDataFunctionSig = entryDataFunction.getSignature();
        if(entryDataFunctionSig.getArgumentCount() < 3)
            throw new XPathException("entry-data function must take at least 3 arguments");
        argTypes = entryDataFunctionSig.getArgumentTypes();
        if(
                argTypes[0].getCardinality() != Cardinality.EXACTLY_ONE || !Type.subTypeOf(Type.ANY_URI, argTypes[0].getPrimaryType()) ||
                argTypes[1].getCardinality() != Cardinality.EXACTLY_ONE || !Type.subTypeOf(Type.STRING, argTypes[1].getPrimaryType()) ||
                argTypes[2].getCardinality() != Cardinality.ZERO_OR_ONE || !Type.subTypeOf(Type.ITEM, argTypes[2].getPrimaryType())
        ) throw new XPathException("entry-data function does not match the expected function signature.");


        Base64Binary compressedData = ((Base64Binary)args[0].itemAt(0));
        
        return processCompressedData(compressedData);
    }

    /**
     * Processes a compressed archive
     *
     * @param compressedData the compressed data to extract
     * @return Sequence of results
     */
    protected abstract Sequence processCompressedData(Base64Binary compressedData) throws XPathException;

    /**
     * Processes a compressed entry from an archive
     *
     * @param name The name of the entry
     * @param isDirectory true if the entry is a directory, false otherwise
     * @param is An InputStream for reading the uncompressed data of the entry
     */
    protected Sequence processCompressedEntry(String name, boolean isDirectory, InputStream is) throws IOException, XPathException
    {
        String dataType = isDirectory ? "folder" : "resource";

        //call the entry-filter function
        Sequence filterParams[] = new Sequence[2];
        filterParams[0] = new AnyURIValue(name);
        filterParams[1] = new StringValue(dataType);
        Sequence entryFilterFunctionResult = entryFilterFunction.evalFunction(contextSequence, null, filterParams);

        if(BooleanValue.FALSE == entryFilterFunctionResult.itemAt(0))
        {
            return Sequence.EMPTY_SEQUENCE;
        }
        else
        {
            Sequence uncompressedData = Sequence.EMPTY_SEQUENCE;

            //copy the input data
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte buf[] = new byte[1024];
            int read = -1;
            while((read = is.read(buf)) != -1)
            {
                baos.write(buf, 0, read);
            }
            byte[] entryData = baos.toByteArray();


            //try and parse as xml, fall back to binary
            try
            {
                uncompressedData = ModuleUtils.streamToXML(context, new ByteArrayInputStream(entryData));
            }
            catch(SAXException saxe)
            {
                if(entryData.length > 0)
                    uncompressedData = new Base64Binary(entryData);
            }

            //call the entry-data function
            Sequence dataParams[] = new Sequence[3];
            System.arraycopy(filterParams, 0, dataParams, 0, 2);
            dataParams[2] = uncompressedData;
            Sequence entryDataFunctionResult = entryDataFunction.evalFunction(contextSequence, null, dataParams);
            
            return entryDataFunctionResult;
        }
    }
}
