package no.nav.nom.libs.lucenewrapper.domain;

import no.nav.nom.libs.lucenewrapper.domain.LuceneOrgenhetFilter.GyldigTomSelection;
import no.nav.nom.libs.lucenewrapper.search.IEntityIndexer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.time.LocalDate;
import java.util.Optional;

import static no.nav.nom.libs.lucenewrapper.domain.LuceneOrgenhetFilter.*;
import static no.nav.nom.libs.lucenewrapper.search.Analyzers.createNGramAnalyzer;
import static no.nav.nom.libs.lucenewrapper.search.Analyzers.createTrimAnalyzer;
import static no.nav.nom.libs.lucenewrapper.search.IEntityIndexer.Utils.analyzerPerFieldCommon;

public class OrgEnhetIndexer implements IEntityIndexer<LuceneOrgEnhet, LuceneOrgenhetFilter> {

    private final Analyzer analyzer = orgenhetAnalyzer();

    private final Utils helper = this.createUtils();

    @Override
    public Document toDoc(LuceneOrgEnhet entity) {
        var doc = helper.createDocumentWithCommonFields(entity.id(), entity.navn());

        doc.add(new IntPoint("gyldigTom", encodedDate(entity.gyldigTom())));
        if (entity.orgNiv() != null) {
            doc.add(new Field("orgniv", entity.orgNiv(), TextField.TYPE_NOT_STORED));
        }

        return doc;
    }

    private String twoDigits(int num) {
        var s = String.valueOf(num);
        if (s.length() < 2) {
            return "0" + s;
        }
        return s;
    }

    private int encodedDate(LocalDate date) {
        var encodedDateStr = Optional.ofNullable(date)
            .map(d -> "" + d.getYear() + twoDigits(d.getMonthValue()) + twoDigits(d.getDayOfMonth()))
            .orElse("99991231");
        return Integer.parseInt(encodedDateStr);
    }

    @Override
    public Query toQuery(String strterms, LuceneOrgenhetFilter filter) throws ParseException {
        if (strterms.isBlank()) {
            return new MatchNoDocsQuery();
        }
        var coreQry = helper.nameQuery(strterms);
        var validDatePassedQry = switch (filter.status()) {

            case IKKE_PASSERT -> IntPoint.newRangeQuery("gyldigTom", encodedDate(LocalDate.now()), Integer.MAX_VALUE);
            case PASSERT -> IntPoint.newRangeQuery("gyldigTom", Integer.MIN_VALUE, encodedDate(LocalDate.now()));
            case ALLE -> new MatchAllDocsQuery();
        };

        var orgNivQry = switch (filter.orgniv()) {

            case ORGNIV_ORGENHET -> helper.termCategoryQuery("orgniv", "ORGENHET");
            case IKKE_ORGNIV_ORGENHET -> helper.termCategoryExcludeQuery("orgniv", "ORGENHET");
            case ALLE -> new MatchAllDocsQuery();
        };

        return helper.requireAllOf(coreQry, validDatePassedQry, orgNivQry);

    }

    @Override
    public String idField() {
        return "orgenhetid";
    }

    @Override
    public String typeString() {
        return "t:orgenhet";
    }

    @Override
    public Analyzer analyzer() {
        return this.analyzer;
    }

    @Override
    public Query toLookupQry(LuceneOrgEnhet ent) {
        return new BooleanQuery.Builder()
            .add(new TermQuery(new Term(this.idField(), ent.id().toLowerCase())), BooleanClause.Occur.MUST)
            .add(new TermQuery(new Term(IEntityIndexer.FIELD_TYPE, this.typeString())), BooleanClause.Occur.MUST)
            .build();
    }

    @Override
    public Class<LuceneOrgEnhet> entityClass() {
        return LuceneOrgEnhet.class;
    }

    @Override
    public Class<LuceneOrgenhetFilter> filterClass() {
        return LuceneOrgenhetFilter.class;
    }

    @Override
    public LuceneOrgenhetFilter defaultFilter() {
        return new LuceneOrgenhetFilter(GyldigTomSelection.ALLE, OrgnivSelection.ALLE);
    }

    private Analyzer orgenhetAnalyzer() {
        var analyzerPerField = analyzerPerFieldCommon(this.idField());
        // Minimum gram size to 2, to better handle terms like HR, IT, etc
        analyzerPerField.put(IEntityIndexer.FIELD_NAME_NGRAM, createNGramAnalyzer(2));
        analyzerPerField.put("orgniv", createTrimAnalyzer());
        return new PerFieldAnalyzerWrapper(createTrimAnalyzer(), analyzerPerField);
    }
}
