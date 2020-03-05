import org.apache.jena.rdf.model.Literal;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws IOException {

        //todo very lenient setting:
        // * if only one match, assume true
        // * only take most matching match


        // IC Subjects from evaluation set
        String multipleMatchesSubsetString = "grape, flax, acanthus, yew, crab, brick, cardinal, apple, curve, ring, " +
                "plate, proof, Dan, barge, bleeding, bishop, bitumen, bed, ensemble, beak, " +
                "cover, composition, housewife, marble, cartoon, globe, torch, exchange, stole, lake, " +
                "sculpture, exhibition, grammar, striking, opening, hood, lambrequin, table, stand, bench, " +
                "justice, shelter, depot, feather, roller, reflection, black and white, corner, equilibrium, " +
                "sewer, bead, choir, end, title, mark, dog, crow, duck, hospital, beech, " +
                "white poplar, pin, embroidery, evening dress, draft, profile, knitting, oak, synagogue, ciborium, " +
                "staining, mule, temple, prison, shoulder, packing, value, oval, state, opera, " +
                "pit, leading, satire, juniper, master, genius, redingote, restoration, series, net, " +
                "smoke, tea, Macedonian, topaz, dark ages, Siren, Er, Dies, Rhea, Atlas";
        //String test = "civilization, cloth, craft, culture, industry, occupations, society, textile industry";
        //String test = "square (shape)";

        List<String> multipleMatchesSubset = new ArrayList<>(Arrays.asList(multipleMatchesSubsetString.split(", ")));

        String outfile = "./data/predicted_matches_th_20.txt";
        String pathToModel = "./data/fuseki-aat-and-ic-oct-2019";
        IcSubjectAatMapper icLabelAatMapper = new IcSubjectAatMapper(pathToModel);

        List<Literal> subjects = icLabelAatMapper.getAllSubjects();

        List<Literal> english_subset = subjects
                .stream()
                .filter(s -> s.getLanguage().equals("en"))
                //.filter(s -> multipleMatchesSubset.contains(s.getLexicalForm()))
                .collect(Collectors.toList());

        icLabelAatMapper.createLinkedEntities(english_subset);

        BufferedWriter writer = new BufferedWriter(new FileWriter(outfile));
        writer.write(icLabelAatMapper.getMatchesString());
        writer.close();


    }


}
