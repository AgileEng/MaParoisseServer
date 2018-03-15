package eu.agileeng.util;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class LightStringTokenizer
{
    private int currIndex;
    private int maxIndex;
    protected String str;
    protected String delimiter;
    protected List<String> tokens = new ArrayList<String>();

    //
    // Constructors
    //
    public LightStringTokenizer(String string, String delim) {
        setStringAndDelimiter(string, delim);
    }

    public LightStringTokenizer(String str) {
        this(str, " ");
    }

    public LightStringTokenizer() {
        this(AEStringUtil.EMPTY_STRING, " ");
    }

    private void reInitialize() {
        tokens.clear();
        split();
        currIndex = 0;
        maxIndex = tokens.size() - 1;
    }

    //
    // API
    //
    public boolean hasMoreTokens() {
        return (currIndex <= maxIndex);
    }

    public String nextToken() {
        if (!hasMoreTokens()) {
            throw new NoSuchElementException();
        }
        return ( (String) tokens.get(currIndex++));
    }

    public void setString(String string) {
        if (string == null) {
            throw new IllegalArgumentException("An argument is null");
        }
        this.str = string;
        reInitialize();
    }

    public void setDelimiter(String delim) {
        if (delim == null) {
            throw new IllegalArgumentException("An argument is null");
        }
        this.delimiter = delim;
        reInitialize();
    }

    public void setStringAndDelimiter(String string, String delim) {
        if (string == null || delim == null) {
            throw new IllegalArgumentException("An argument is null");
        }
        this.str = string;
        this.delimiter = delim;
        reInitialize();
    }

    protected void split() {
        int pos1 = 0;
        int pos = 0;
        String newString = str;

        // do this loop as long as you have a delimiter
        do {
            // set to zero
            pos1 = 0;

            //position of delimiter starting at pos1 (0)
            pos = newString.indexOf(delimiter, pos1);
            if (pos != -1) {
                //load a new var with the info left of the position
                String token = newString.substring(pos1, pos);
                tokens.add(token);

                //make the remain string to split
                newString = newString.substring(pos + delimiter.length());
            }
        } while (pos != -1);

        //newString is the string right of the last deliminator, add it
        if (!AEStringUtil.isEmpty(newString)) {
            tokens.add(newString);
        }
    }
}