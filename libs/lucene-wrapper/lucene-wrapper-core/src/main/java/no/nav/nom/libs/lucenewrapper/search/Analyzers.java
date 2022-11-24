package no.nav.nom.libs.lucenewrapper.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.ngram.EdgeNGramTokenFilter;
import org.apache.lucene.analysis.phonetic.DoubleMetaphoneFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import java.util.HashMap;

public interface Analyzers {


    public static Analyzer createNGramAnalyzer(){
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer source = new StandardTokenizer();
                TokenStream result = new LowerCaseFilter(source);
                result = new EdgeNGramTokenFilter(result ,3,40,false);
                return new TokenStreamComponents(source, result);
            }
        };
    }

    public static Analyzer createMetaphoneAnalyzer(){
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer source = new StandardTokenizer();
                TokenStream result = new LowerCaseFilter(source);
                result = new DoubleMetaphoneFilter(result ,10,false);
                return new TokenStreamComponents(source, result);
            }
        };
    }


    public static Analyzer createPerFieldAnalyzer(HashMap<String,Analyzer> fieldToAnalyzer) {
        return new PerFieldAnalyzerWrapper(createSimpleIgnoreCaseAnalyzer(), fieldToAnalyzer);
    }

    public static Analyzer createSimpleIgnoreCaseAnalyzer(){
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer source = new WhitespaceTokenizer();
                TokenStream result = new LowerCaseFilter(source);
                result = new ASCIIFoldingFilter(result);
                return new TokenStreamComponents(source, result);
            }
        };
    }
}
