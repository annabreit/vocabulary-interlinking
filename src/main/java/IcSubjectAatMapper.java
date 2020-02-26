import org.apache.jena.rdf.model.Literal;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class IcSubjectAatMapper extends IcAatMapper {
    // context lvl 1: terms in parenthesis
    // context lvl 2: direct sibling tags (tags that are connected to same iconclass, level dependent)
    // context lvl 3: all sibling tags (tags that are connected to same iconclass, including parents)

    private static final Pattern IC_SUBJECT_LABEL_PATTERN = Pattern.compile("(.*)\\s*\\((.*)\\)");
    private Map<String, IcSubject> labelToSubject = new HashMap<>();
    public IcSubjectAatMapper(String pathToModel) {
        super(pathToModel);
    }

    public void createLinkedEntities() {
        List<Literal> subjectsList = getAllSubjects();
        createLinkedEntities(subjectsList);
    }

    public void createLinkedEntities(List<Literal> subjectsList) {

        int multiple = 0;
        int single = 0;
        int none = 0;
        System.out.println(subjectsList.size());
        for (Literal subject : subjectsList) {
            IcSubject icSubject = new IcSubject(subject);
            labelToSubject.put(subject.getLexicalForm(), icSubject);

            List<RightEntity> candidateMatches = new ArrayList<>();

            for (Label l : icSubject.labels) {
                candidateMatches.addAll(getExactAnyLabelAATMatch(l.getPreprocessed_string(), l.getLanguage()) //info change here for contain
                        .stream()
                        .map(matchedUri -> new AatConcept(matchedUri))
                        .collect(Collectors.toList()));
            }
            // disambiguate
            System.out.println();
            System.out.println("IC subject: " + subject.getLexicalForm());
            System.out.println("candidates " + candidateMatches.stream().map(r -> r.identifier).collect(Collectors.toList()));

            matches.put(icSubject, disambiguateMatches(icSubject, candidateMatches));

            System.out.println("matches" + matches.get(icSubject).stream().map(r -> r.identifier).collect(Collectors.toList()));

            if (candidateMatches.size() > 1) {
                multiple++;
            } else if (candidateMatches.size() == 1) {
                single++;
            } else none++;

        }
        System.out.println(none);
        System.out.println(single);
        System.out.println(multiple);
    }


    class IcSubject extends LeftEntity {

        public IcSubject(Literal subject) {
            this(subject.getLexicalForm(), subject.getLanguage());
        }

        public IcSubject(String label, String language) {
            super(label);
            Label l = new Label(label, language);

            Matcher m = IC_SUBJECT_LABEL_PATTERN.matcher(label);

            // context lvl 1: terms in parenthesis
            if (m.find()) {
                String processed_label = m.group(1);
                this.setLvl1context(Arrays
                        .stream(m.group(2).split(",", 0))
                        .map(str -> str.trim())
                        .collect(Collectors.toList()));
                l.setPreprocessed_string(processed_label);
            }
            this.labels = Arrays.asList(l);

            // context lvl 2: direct sibling tags (tags that are connected to same iconclass, level dependent)
            List<String> iconclassUrisWithSubject = getIconClassUrisForSubject(this.identifier)
                    .stream()
                    .map(resource -> resource.getURI())
                    .collect(Collectors.toList());
            List<String> siblingTags = new ArrayList<>();
            List<String> inheritedTags = new ArrayList<>();
            for (String icUri : iconclassUrisWithSubject) {
                String parentUri = getParentForIconclass(icUri);
                List<String> inheritedSubjects = getSubjectsForUri(parentUri)
                        .stream()
                        .filter(literal -> literal.getLanguage().equals(language))
                        .map(literal -> literal.getLexicalForm())
                        .collect(Collectors.toList());
                siblingTags.addAll(getSubjectsForUri(icUri)
                        .stream()
                        .filter(literal -> literal.getLanguage().equals(language))
                        .map(literal -> literal.getLexicalForm())
                        .filter(subjectString -> !inheritedSubjects.contains(subjectString))
                        .collect(Collectors.toList()));
                inheritedTags.addAll(inheritedSubjects);
            }

            siblingTags.remove(label);

            setLvl2context(siblingTags);
            this.lvl2context = new ArrayList<>(new HashSet<>(this.lvl2context));


            // context lvl 3: all sibling tags (tags that are connected to same iconclass, including parents)
            setLvl3context(inheritedTags);
            this.lvl3context = new ArrayList<>(new HashSet<>(this.lvl3context));


        }

    }

}
