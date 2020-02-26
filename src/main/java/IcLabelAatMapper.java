import java.util.*;
import java.util.stream.Collectors;

public class IcLabelAatMapper extends IcAatMapper {
    // context lvl 1: direct subjects
    // context lvl 2: other subjects
    // context lvl 3: other labels with similar subjects
    private Map<String, IconClass> uriToIconClass = new HashMap<>();

    public IcLabelAatMapper(String pathToModel) {
        super(pathToModel);
    }

    @Override
    public void createLinkedEntities() {


        List<String> iconClassesUris = getAllIconclassUris();

        for (String iconClassUri : iconClassesUris) {
            IconClass iconClass = new IconClass(iconClassUri);
            uriToIconClass.put(iconClassUri, iconClass);

            List<RightEntity> candidateMatches = new ArrayList<>();

            // try matching language specific labels
            // treat alt and pref labels equally, as recall should be increased
            // filter for language, as the same word may have VERY different meaning in other languages, e.g. arts=physician in nl
            for (Label prefLabel : iconClass.labels) {
                String labelString = prefLabel.getPreprocessed_string();
                String languageString = prefLabel.getLanguage();
                candidateMatches.addAll(getContainAnyLabelAATMatch(labelString, languageString)
                        .stream()
                        .map(matchedUri -> new AatConcept(matchedUri))
                        .collect(Collectors.toList()));
            }
            // disambiguate
            matches.put(iconClass, disambiguateMatches(iconClass, candidateMatches));

            if (candidateMatches.size() > 1) {
                System.out.println("candidate " + (iconClass).identifier);
                System.out.println(candidateMatches.stream().map(r -> r.identifier).collect(Collectors.toList()));
            }


        }
    }


    class IconClass extends LeftEntity {

        public IconClass(String uri) {
            super(uri);
            this.lvl2context = getSubjectsForUri(uri)
                    .stream()
                    .map(apache_literal -> new ContextElement(Arrays.asList(new Label(apache_literal.getLexicalForm(), apache_literal.getLanguage()))))
                    .collect(Collectors.toList());
            //improve take into account the hierarchy? Or should we not use the hierarchy at all since it is completely different anyway?
            this.labels = getPrefLabels(uri)
                    .stream()
                    .map(apache_literal -> new Label(apache_literal.getLexicalForm(), apache_literal.getLanguage()))
                    .collect(Collectors.toList());
            for (Label l : this.labels) {
                String label = l.getOriginal_string();
                label = label.replaceAll("[()]", " ");
                label = label.replaceAll("[~]", " ");
                label = label.replaceAll("['\"]", " ");
                l.setPreprocessed_string(label);
            }
        }

    }
}
