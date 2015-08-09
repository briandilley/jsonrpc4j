package com.googlecode.jsonrpc4j.loadtest;

import java.util.List;

/**
 * @author Eduard Szente
 */
public interface JsonRpcService {
	void doSomething();
    
     int returnSomeSimple(int arg);
     
     ComplexType returnSomeComplex(int arg1, String arg2);
     
     void throwSomeException(String message)
         throws Exception;
     
     class ComplexType {
     
        private int    integer;
        private String string;
        private List<String> list;

        public int getInteger()
        {
            return integer;
        }

        public void setInteger(int integer)
        {
            this.integer = integer;
        }

        public String getString()
        {
            return string;
        }

        public void setString(String string)
        {
            this.string = string;
        }

        public List<String> getList()
        {
            return list;
        }

        public void setList(List<String> list)
        {
            this.list = list;
        }
     }
}
