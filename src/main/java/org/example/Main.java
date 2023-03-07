import java.io.File;
import java.util.*;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import gate.*;
import gate.creole.*;
import gate.util.*;
import gate.util.persistence.*;

public class Main {
    public static void main(String[] args) throws Exception{

        // Initialisiere GATE
        Gate.init();

        // Lade das ANNIE-Plugin in die pipeline
        ConditionalSerialAnalyserController pipeline = (ConditionalSerialAnalyserController) PersistenceManager.loadObjectFromFile(new File("ANNIE_config.gapp"));


        // Hier muss der Pfad zu den Dokumenten angegeben werden. Zulässig sind sowohl lokale Pfade als auch URLs.
        // Folgende Formate sind zulässig: ...
        String Dokument_1 = "C:\\Users\\ImageMaster 9 - Compatibility Guide_9.11.2.pdf";
        String Dokument_2 = "C:\\Users\\ImageMaster 9 - Compatibility Guide_9.12.2.pdf";



        // Initialisierung der Dokumente und des Ausgabepfades
        Document doc1 = null;
        Document doc2 = null;

        File htmlFile1 = new File("Datei_1.html");
        File htmlFile2 = new File("Datei_2.html");

        // Prüfe, ob die Dokumente als URL oder als lokale Datei angegeben wurden. Je nachdem werden die Pfade verarbeitet.
        // Die Dokumente werden als HTML-Dateien gespeichert.
        if ((Dokument_1.startsWith("http://") || Dokument_2.startsWith("http://") || (Dokument_1.startsWith("https://") || Dokument_2.startsWith("https://")))) {

            URL doc1Url = new URL(Dokument_1);
            doc1 = Factory.newDocument(doc1Url);

            URL doc2Url = new URL(Dokument_2);
            doc2 = Factory.newDocument(doc2Url);

            save_URL_in_File(Dokument_1, "Datei_1.html");
            save_URL_in_File(Dokument_2, "Datei_2.html");

        } else  {

            File doc1File = new File(Dokument_1);
            doc1 = Factory.newDocument(doc1File.toURL());

            File doc2File = new File(Dokument_2);
            doc2 = Factory.newDocument(doc2File.toURL());

            convert_to_Html(doc1, "Datei_1.html");
            convert_to_Html(doc2, "Datei_2.html");
        }

        // Dokumente werden dem Corpus hinzugefügt und die Pipeline wird ausgeführt.
        pipeline.setCorpus(Factory.newCorpus("Corpus"));
        pipeline.getCorpus().add(doc1);
        pipeline.getCorpus().add(doc2);
        pipeline.execute();


        // Die Named Entities werden in die Listen entities1 und entities2 geschrieben.
        NamedEntities_in_document(doc1, entities1);
        NamedEntities_in_document(doc2, entities2);

        // Die POS-Tags werden in die Listen entities3 und entities4 geschrieben.
        POS_in_document(doc1, doc2, entities3, entities4);


        // Die Unterschiede zwischen entities3 und entities4 werden in die Liste diff_POS_List geschrieben.
        List<List<Object>> diff_POS_List = difference_POS(entities3, entities4);
        List<String> differences_Part_of_Speech = new ArrayList<>();
        for (List<Object> difference : diff_POS_List) {
            differences_Part_of_Speech.add((String) difference.get(0));
        }

        // Die Unterschiede zwischen entities1 und entities2 werden in die Liste geschrieben.
        List<String> differences_Named_Entities = difference_NER(entities1, entities2);


        // Die Unterschiede werden in den HTML-Dateien hervorgehoben.
        highlight_in_Html(htmlFile1, differences_Named_Entities);
        highlight_in_Html(htmlFile2,differences_Named_Entities);

        highlight_in_Html(htmlFile1, differences_Part_of_Speech);
        highlight_in_Html(htmlFile2, differences_Part_of_Speech);


        // Die Ressourcen werden wieder freigegeben.
        Factory.deleteResource(pipeline);
        Factory.deleteResource(doc1);
        Factory.deleteResource(doc2);

    }

    // Initialisierung der Listen, in die die Entitäten geschrieben werden.
    private static List<List<String>> entities1 = new ArrayList<>();
    private static List<List<String>> entities2 = new ArrayList<>();

    private static List<List<Object>> entities3 = new ArrayList<>();
    private static List<List<Object>> entities4 = new ArrayList<>();

    private static void convert_to_Html(Document doc, String outputPath) {

        try {
            // Konvertierung in HTML
            String content = doc.toXml(doc.getAnnotations(), true);
            String html = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" " +
                    "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">" +
                    "<html xmlns=\"http://www.w3.org/1999/xhtml\">" +
                    "<head><title>" + doc.getName() + "</title></head>" +
                    "<body>" + content + "</body></html>";

            // Speichern der HTML-Datei
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));
            writer.write(html);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void save_URL_in_File(String urlString, String filename) throws IOException {

        // Speichern der URL in einer Datei
        URL url = new URL(urlString);
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

        // Speichern der Datei
        FileWriter writer = new FileWriter(filename);
        String line;
        while ((line = reader.readLine()) != null) {
            writer.write(line);
        }

        reader.close();
        writer.close();
    }
    private static void NamedEntities_in_document(Document doc, List<List<String>> entities) {
        // Zugriff auf die Annotationen
        AnnotationSet defaultAnnotSet = doc.getAnnotations();
        Set<String> annotTypesRequired = new HashSet<>(Arrays.asList("Person", "Date", "Organization", "Location", "Money", "Time", "Email", "Url", "Number", "Percent", "Address", "Identifier"));
        // Iteration über alle Annotationen
        for (String s : annotTypesRequired) {
            AnnotationSet annotSet = defaultAnnotSet.get(s);

            if (annotSet == null) continue;

            // Zugriff auf alle Kategorien für Named Entities
            for (Annotation annot : annotSet) {
                FeatureMap features = annot.getFeatures();
                String entityType = (String) features.get("category") != null ? (String) features.get("category") : "Person";
                if (s.equals("Date")) entityType = "Date";
                if (s.equals("Organization")) entityType = "Organization";
                if (s.equals("Location")) entityType = "Location";
                if (s.equals("Money")) entityType = "Money";
                if (s.equals("Time")) entityType = "Time";
                if (s.equals("Email")) entityType = "Email";
                if (s.equals("Url")) entityType = "Url";
                if (s.equals("Number")) entityType = "Number";
                if (s.equals("Percent")) entityType = "Percent";
                if (s.equals("Address")) entityType = "Address";
                if (s.equals("Identifier")) entityType = "Identifier";
                //if (s.equals("Lookup")) entityType = "Lookup";
                //if (s.equals("Sentence")) entityType = "Sentence";

                // Speichern der Kategorie und des Textes
                try {
                    String text = doc.getContent().getContent(annot.getStartNode().getOffset(), annot.getEndNode().getOffset()).toString();
                    List<String> entity = new ArrayList<>();
                    entity.add(entityType);
                    entity.add(text);
                    entities.add(entity);
                } catch (InvalidOffsetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void POS_in_document(Document doc1, Document doc2, List<List<Object>> entities1, List<List<Object>> entities2) {

        // Zugriff auf die Annotationen
        AnnotationSet annotationSet1 = doc1.getAnnotations().get("Token");
        AnnotationSet annotationSet2 = doc2.getAnnotations().get("Token");

        // Iteration über alle Annotationen
        for (Annotation annotation : annotationSet1) {
            Long startOffset = annotation.getStartNode().getOffset();
            FeatureMap featureMap = annotation.getFeatures();
            String word = (String) featureMap.get("string");
            String posTag = (String) featureMap.get("category");

            // Nur die festgelegten POS-Tags werden gespeichert.
            if (posTag != null && (posTag.startsWith("NN") || posTag.equals("VB") || posTag.equals("JJ") || posTag.equals("RB") || posTag.equals("PRP") || posTag.equals("RBR") || posTag.equals("CD")) && word.length() > 1) {
                List<Object> entity = new ArrayList<>();
                entity.add(posTag);
                entity.add(word);
                entity.add(startOffset);
                entity.add(startOffset + word.length());
                entities1.add(entity);
            }
        }

        // Iteration über alle Annotationen
        for (Annotation annotation : annotationSet2) {
            Long startOffset = annotation.getStartNode().getOffset();
            FeatureMap featureMap = annotation.getFeatures();
            String word = (String) featureMap.get("string");
            String posTag = (String) featureMap.get("category");
            if (posTag != null && (posTag.startsWith("NN") || posTag.equals("VB") || posTag.equals("JJ") || posTag.equals("RB") || posTag.equals("PRP") || posTag.equals("RBR") || posTag.equals("CD") || posTag.equals("NNP")) && word.length() > 1) {
                List<Object> entity = new ArrayList<>();
                entity.add(posTag);
                entity.add(word);
                entity.add(startOffset);
                entity.add(startOffset + word.length());
                entities2.add(entity);
            }
        }
    }




    public static List<String> difference_NER(List<List<String>> entities1, List<List<String>> entities2){
        List<String> differences = new ArrayList<>();

        // Iteration über alle Entititäten von entities1
        for (List<String> entity1 : entities1) {
            boolean found = false;
            for (List<String> entity2 : entities2) {
                if (entity1.get(0).equals(entity2.get(0)) && entity1.get(1).equals(entity2.get(1))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                differences.add(entity1.get(1));
            }
        }
        // Iteration über alle Entititäten von entities2
        for (List<String> entity2 : entities2) {
            boolean found = false;
            for (List<String> entity1 : entities1) {
                if (entity2.get(0).equals(entity1.get(0)) && entity2.get(1).equals(entity1.get(1))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                differences.add(entity2.get(1));
            }
        }
        return differences;
    }

    public static List<List<Object>> difference_POS(List<List<Object>> entities1, List<List<Object>> entities2) {
        List<List<Object>> differences = new ArrayList<>();

        // Iteration über alle Entititäten von entities1
        for (List<Object> entity1 : entities1) {
            boolean found = false;
            for (List<Object> entity2 : entities2) {
                if (entity1.get(0).equals(entity2.get(0))) {
                    if (entity1.get(1).equals(entity2.get(1))) {
                        found = true;
                        break;
                    } else if ((Long) entity2.get(2) - (Long) entity1.get(2) > 150) {
                        found = true;
                        break;
                    }
                }
            }

            // Unterschiede werden gespeichert, mitsamt der Position
            if (!found) {
                List<Object> difference = new ArrayList<>();
                difference.add(entity1.get(1));
                difference.add(entity1.get(2));
                difference.add(entity1.get(3));
                differences.add(difference);
            }
        }

        // Iteration über alle Entititäten von entities2
        for (List<Object> entity2 : entities2) {
            boolean found = false;
            for (List<Object> entity1 : entities1) {
                if (entity2.get(0).equals(entity1.get(0))) {
                    if (entity2.get(1).equals(entity1.get(1))) {
                        found = true;
                        break;
                    } else if ((Long) entity1.get(2) - (Long) entity2.get(2) > 150) {
                        found = true;
                        break;
                    }
                }
            }

            // Unterschiede werden gespeichert, mitsamt der Position
            if (!found) {
                List<Object> difference = new ArrayList<>();
                difference.add(entity2.get(1));
                difference.add(entity2.get(2));
                difference.add(entity2.get(3));
                differences.add(difference);
            }
        }
        return differences;
    }

// Diese Methode ist noch nicht fertig und funktioniert nur, wenn beide Dokumente im Pdf-Format vorliegen
/*
    public static void search_for_Synonyms(List<List<Object>> entities1, List<List<Object>> entities2) {

        Map<String, Integer> dictionary = new HashMap<>();
        List<String> allWords = entities1.stream()
                .map(entity -> (String) entity.get(1))
                .collect(Collectors.toList());
        allWords.addAll(entities2.stream()
                .map(entity -> (String) entity.get(1))
                .collect(Collectors.toList()));
        for (String word : allWords) {
            dictionary.compute(word, (key, value) -> (value == null) ? 1 : value + 1);
        }


        int numWords = dictionary.size();
        int numDims = 100; // word vector dimensionality
        RealMatrix matrix = new Array2DRowRealMatrix(numWords, numDims);
        int i = 0;
        Map<String, Integer> wordIndices = new HashMap<>();
        for (String word : dictionary.keySet()) {
            RealVector vec = new ArrayRealVector(numDims);
            double[] values = new double[numDims];
            for (int j = 0; j < numDims; j++) {
                values[j] = Math.random();
            }
            vec = new ArrayRealVector(values);
            matrix.setRow(i, vec.toArray());
            wordIndices.put(word, i);
            i++;
        }
        SingularValueDecomposition svd = new SingularValueDecomposition(matrix);
        RealMatrix wordVectors = svd.getU().getSubMatrix(0, numWords - 1, 0, numDims - 1);


        for (List<Object> entity1 : entities1) {
            String word1 = (String) entity1.get(1);
            RealVector vec1 = get_Vector(word1, wordIndices, wordVectors);
            for (List<Object> entity2 : entities2) {
                String word2 = (String) entity2.get(1);
                RealVector vec2 = get_Vector(word2, wordIndices, wordVectors);
                double similarity = vec1.dotProduct(vec2) / (vec1.getNorm() * vec2.getNorm());
                if ((similarity > 0.9) && (similarity < 0.99))  {
                    System.out.println(word1 + " and " + word2 + " are synonyms");
                }
            }
        }
    }

    private static RealVector get_Vector(String word, Map<String, Integer> wordIndices, RealMatrix wordVectors) {
        Integer index = wordIndices.get(word);
        if (index == null) {
            throw new IllegalArgumentException("Word not found in dictionary: " + word);
        }
        if (wordVectors.getColumnDimension() < 100) {
            throw new IllegalArgumentException("Word vectors matrix is too small");
        }
        double[] values = wordVectors.getRow(index);
        return new ArrayRealVector(values);
    }
    */
    public static void highlight_in_Html(File htmlFile, List<String> list1) throws IOException {
        // Lese den Inhalt der HTML-Datei
        BufferedReader reader = new BufferedReader(new FileReader(htmlFile));
        StringBuilder htmlBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            htmlBuilder.append(line).append("\n");
        }
        String htmlContent = htmlBuilder.toString();
        reader.close();

        // Wort wird durch hervorgehobenes Wort ersetzt
        for (String word : list1) {
            htmlContent = htmlContent.replaceAll("\\b" + word + "\\b", "<span style='background-color: yellow'>" + word + "</span>");
        }

        // Schreibt "neuen" Inhalt in die HTML-Datei
        FileWriter writer = new FileWriter(htmlFile);
        writer.write(htmlContent);
        writer.close();
    }
}