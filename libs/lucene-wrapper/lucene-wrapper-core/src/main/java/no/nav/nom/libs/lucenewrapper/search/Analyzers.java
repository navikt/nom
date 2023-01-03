package no.nav.nom.libs.lucenewrapper.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.miscellaneous.TrimFilter;
import org.apache.lucene.analysis.ngram.EdgeNGramTokenFilter;
import org.apache.lucene.analysis.phonetic.DoubleMetaphoneFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import java.util.HashMap;

public interface Analyzers {


    static Analyzer createNGramAnalyzer(int mingram) {
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer source = new StandardTokenizer();
                TokenStream result = new EdgeNGramTokenFilter(new LowerCaseFilter(source), mingram, 40, false);
                return new TokenStreamComponents(source, result);
            }
        };
    }

    static Analyzer createNGramAnalyzer() {
        return createNGramAnalyzer(3);
    }

    static Analyzer createMetaphoneAnalyzer() {
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer source = new StandardTokenizer();
                TokenStream result = new DoubleMetaphoneFilter(new LowerCaseFilter(source), 10, false);
                return new TokenStreamComponents(source, result);
            }
        };
    }


    static Analyzer createPerFieldAnalyzer(HashMap<String, Analyzer> fieldToAnalyzer) {
        return new PerFieldAnalyzerWrapper(createSimpleIgnoreCaseAnalyzer(), fieldToAnalyzer);
    }

    static Analyzer createSimpleIgnoreCaseAnalyzer() {
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer source = new WhitespaceTokenizer();
                TokenStream result = new ASCIIFoldingFilter(new LowerCaseFilter(source));
                return new TokenStreamComponents(source, result);
            }
        };
    }

    static Analyzer createTrimAnalyzer() {
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName){
                Tokenizer source = new WhitespaceTokenizer();
                TokenStream result = new TrimFilter(source);
                return new Analyzer.TokenStreamComponents(source,result);
            }
        };
    }

    static Analyzer createKeywordAnalyzer() {
        return new KeywordAnalyzer();
    }
}
