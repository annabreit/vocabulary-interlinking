import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.tdb.TDBFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public abstract class IcAatMapper extends EntityMapper {
    private Model dbModel;

    public IcAatMapper(String pathToModel) {
        this.dbModel = TDBFactory.createDataset(pathToModel).getDefaultModel();
    }


    // -------------- Icon Class ---------------------------

    public List<String> getAllIconclassUris() {
        return getAllUrisFromScheme("http://iconclass.org/rdf/2011/09/");
    }

    public String getParentForIconclass(String uri) {
        String parentUri;
        if (uri.endsWith("%29")) {
            int indexOpeningParenthesis = uri.lastIndexOf("%28");
            parentUri = uri.substring(0, indexOpeningParenthesis);
        } else {
            parentUri = uri.substring(0, (uri.length() - 1));
        }
        return parentUri;
    }

    public List<Literal> getAllSubjects() {
        String queryString = "SELECT  DISTINCT  ?o" +
                "    WHERE {" +
                "  ?original_string <http://purl.org/dc/elements/1.1/subject> ?o." +
                "    }";
        return ResultSetFormatter.toList(getQueryResults(queryString)).stream().map(resultSet -> resultSet.getLiteral("o")).collect(Collectors.toList());
    }

    public List<Literal> getSubjectsForUri(String uri) {
        String queryString = "SELECT  DISTINCT  ?o" +
                "    WHERE {" +
                "  <" + uri + "> <http://purl.org/dc/elements/1.1/subject> ?o." +
                "    }";
        return ResultSetFormatter.toList(getQueryResults(queryString)).stream().map(resultSet -> resultSet.getLiteral("o")).collect(Collectors.toList());
    }

    public List<Resource> getIconClassUrisForSubject(String label) {
        String queryString = "SELECT  DISTINCT  ?original_string" +
                "    WHERE {" +
                "  ?original_string <http://purl.org/dc/elements/1.1/subject> ?o." +
                "   FILTER(STR(?o)= \"" + label + "\" )" +
                "    }";
        return ResultSetFormatter.toList(getQueryResults(queryString)).stream().map(resultSet -> resultSet.getResource("original_string")).collect(Collectors.toList());
    }

    public List<Literal> getPrefLabels(String uri) {
        String queryString = "SELECT  DISTINCT  ?o" +
                "    WHERE {" +
                "  <" + uri + "> <http://www.w3.org/2004/02/skos/core#prefLabel> ?o." +
                "    }";
        return ResultSetFormatter.toList(getQueryResults(queryString)).stream().map(resultSet -> resultSet.getLiteral("o")).collect(Collectors.toList());
    }

    public List<String> getAllUrisFromScheme(String schemeUri) {
        String queryString = " SELECT  DISTINCT ?original_string" +
                "    WHERE {" +
                "        ?original_string  <http://www.w3.org/2004/02/skos/core#inScheme> <" + schemeUri + ">." +
                "    }";
        ResultSet resultSet = getQueryResults(queryString);
        List<String> uris = ResultSetFormatter.toList(resultSet).stream().map(ic -> ic.getResource("?original_string").getURI()).collect(Collectors.toList());
        return uris;
    }


    // ------------------- AAT ---------------------------

    public List<String> getExactPrefLabelAATMatch(String label) {
        return getExactPrefLabelAATMatch(label, "");
    }

    public List<String> getExactPrefLabelAATMatch(String label, String language) {
        String languageFilterQuerString = "";
        if (!language.equals("")) {
            languageFilterQuerString = "FILTER(LANG(?identifier)=\"" + language + "\").";
        }
        String queryString = "SELECT  DISTINCT  ?original_string" +
                "    WHERE {" +
                " ?original_string <http://vocab.getty.edu/ontology#prefLabelGVP> ?o ." +
                "  ?o <http://vocab.getty.edu/ontology#term> ?identifier." +
                "  FILTER(regex(str(?identifier) ,\"^" + label + "$\", \"i\")).  " +      //improve look into LARQ for speedup https://stackoverflow.com/questions/10660030/how-to-write-sparql-query-that-efficiently-matches-string-literals-while-ignorin
                languageFilterQuerString +
                "    }";
        ResultSet resultSet = getQueryResults(queryString);
        List<String> aatConcepts = ResultSetFormatter.toList(resultSet).stream().map(ic -> ic.getResource("?original_string").getURI()).collect(Collectors.toList());
        return aatConcepts;
    }

    public List<String> getExactAnyLabelAATMatch(String label, String language) {
        String languageFilterQueryString = "";
        if (!language.equals("")) {
            languageFilterQueryString = "FILTER(LANG(?identifier)=\"" + language + "\").";
        }
        String queryString = "SELECT  DISTINCT  ?original_string" +
                "    WHERE {" +
                " ?original_string ?r ?o ." +
                "  ?o <http://vocab.getty.edu/ontology#term> ?identifier." +
                "  FILTER(regex(str(?identifier) ,\"^" + label + "$\", \"i\")).  " +      //improve look into LARQ for speedup https://stackoverflow.com/questions/10660030/how-to-write-sparql-query-that-efficiently-matches-string-literals-while-ignorin
                languageFilterQueryString +
                "    }";
        ResultSet resultSet = getQueryResults(queryString);
        List<String> aatConcepts = ResultSetFormatter.toList(resultSet)
                .stream()
                .map(ic -> ic.getResource("?original_string").getURI())
                .collect(Collectors.toList());
        return aatConcepts;
    }

    public List<String> getAATParentStringsForUri(String uri) {
        String queryString = "SELECT  ?parentString" +
                "    WHERE {" +
                " <" + uri + "> <http://vocab.getty.edu/ontology#parentString> ?parentString ." +
                "     }";
        ResultSet resultSet = getQueryResults(queryString);
        String parentString = ResultSetFormatter.toList(resultSet).get(0).getLiteral("?parentString").getLexicalForm();
        List parents;
        parents = Arrays.asList((parentString.split(",")));

        return parents;
    }

    public List<String> getContainAnyLabelAATMatch(String label, String language) {
        String languageFilterQuerString = "";
        if (!language.equals("")) {
            languageFilterQuerString = "FILTER(LANG(?identifier)=\"" + language + "\").";
        }
        String queryString = "SELECT  DISTINCT  ?original_string" +
                "    WHERE {" +
                " ?original_string ?r ?o ." +
                "  ?o <http://vocab.getty.edu/ontology#term> ?identifier." +
                "  FILTER(regex(str(?identifier) ,\"" + label + "\", \"i\")).  " +      //improve look into LARQ for speedup https://stackoverflow.com/questions/10660030/how-to-write-sparql-query-that-efficiently-matches-string-literals-while-ignorin
                //"  FILTER(CONTAINS(str(?identifier) ,\"" +label + "\")).  " +
                languageFilterQuerString +
                "    }";
        ResultSet resultSet = getQueryResults(queryString);
        List<String> aatConcepts = ResultSetFormatter.toList(resultSet).stream().map(ic -> ic.getResource("?original_string").getURI()).collect(Collectors.toList());
        return aatConcepts;
    }

    public List<Literal> getAATPrefLabel(String uri) {
        String queryString = "SELECT  DISTINCT  ?identifier" +
                "    WHERE {" +
                "  <" + uri + "> <http://vocab.getty.edu/ontology#prefLabelGVP> ?o." +
                " ?o <http://vocab.getty.edu/ontology#term> ?identifier ." +
                "    }";
        return ResultSetFormatter.toList(getQueryResults(queryString)).stream().map(resultSet -> resultSet.getLiteral("identifier")).collect(Collectors.toList());
    }

    public List<Literal> getAATAltLabels(String uri) {
        return getAATAltLabels(uri, "");
    }

    public List<Literal> getAATAltLabels(String uri, String language) {
        String languageFilterQuerString = "";
        if (!language.equals("")) {
            languageFilterQuerString = "FILTER(LANG(?identifier)=\"" + language + "\").";
        }
        String queryString = "SELECT  DISTINCT  ?identifier" +
                "    WHERE {" +
                "  <" + uri + "> <http://www.w3.org/2008/05/skos-xl#altLabel> ?o." +
                " ?o <http://vocab.getty.edu/ontology#term> ?identifier ." +
                languageFilterQuerString +
                "    }";
        return ResultSetFormatter.toList(getQueryResults(queryString)).stream().map(resultSet -> resultSet.getLiteral("identifier")).collect(Collectors.toList());
    }


    public List<String> getAATBroaderPreferred(String uri) {
        String queryString = "SELECT  DISTINCT  ?o" +
                "    WHERE {" +
                "  <" + uri + "> <http://vocab.getty.edu/ontology#broaderPreferred> ?o." +
                "    }";
        return ResultSetFormatter.toList(getQueryResults(queryString)).stream().map(resultSet -> resultSet.getResource("?o").getURI()).collect(Collectors.toList());
    }


    // ------------------- General ---------------------------

    public ResultSet getQueryResults(String queryString) {
        QueryExecution qexec = QueryExecutionFactory.create(queryString, this.dbModel);
        ResultSet results = qexec.execSelect();
        //improve how to close? qexec.close();
        return results;
    }


// ########################################################################################################################

    // context: all parent-labels
    class AatConcept extends RightEntity {

        public AatConcept(String uri) {
            this(uri, getAATPrefLabel(uri).get(0).getLexicalForm(), getAATPrefLabel(uri).get(0).getLanguage());
        }

        public AatConcept(String uri, String label, String language) {
            super(uri, label);
            List<String> parentLabels = new ArrayList<>();
            List<String> broaderResult = getAATBroaderPreferred(uri);

            while (broaderResult.size() > 0) {
                String parentUri = broaderResult.get(0);
                parentLabels.addAll(getAATPrefLabel(parentUri)
                        .stream()
                        .map(literal -> literal.getLexicalForm())
                        .collect(Collectors.toList()));
                parentLabels.addAll(getAATAltLabels(parentUri, language)
                        .stream()
                        .map(literal -> literal.getLexicalForm())
                        .collect(Collectors.toList()));
                broaderResult = getAATBroaderPreferred(parentUri);
            }

            List<ContextElement> parents = parentLabels
                    .stream()
                    .map(s -> new ContextElement(new ArrayList<>(Arrays.asList(new Label(s)))))
                    .collect(Collectors.toList());
            setContext(parents);


            // -------- alternatively: take the parent string ---------
            /*
            List<String> rawParentStrings = getAATParentStringsForUri(uri);
            List<String> parentStrings = new ArrayList<>();
            final Pattern PATTERN = Pattern.compile("(.*)\\s*\\((.*)\\)");

            for (String rawParentString : rawParentStrings){
                Matcher m = PATTERN.matcher(rawParentString);
                if (m.find()){
                    parentStrings.add(m.group(1).trim());
                    parentStrings.add(m.group(2).trim());
                }
                else{
                    parentStrings.add(rawParentString.trim());
                }
            }

            List<ContextElement> parents = parentStrings
                    .stream()
                    .map(s -> new ContextElement(new ArrayList<>(Arrays.asList(new Label(s)))))
                    .collect(Collectors.toList());

             */


            //TODO
            // * add related concepts to context

        }

    }


}
