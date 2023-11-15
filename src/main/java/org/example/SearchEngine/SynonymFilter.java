package org.example.SearchEngine;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class SynonymFilter {
    public Map<String, List<String>> synonymMap;
    private Analyzer analyzer;

    public SynonymFilter(Analyzer analyzer) throws IOException, ParseException {
        synonymMap = new TreeMap<>();
        this.analyzer = analyzer;
        parseSynonymsDict();
    }

    private void parseSynonymsDict() throws FileNotFoundException, ParseException {
        // Укажите путь к вашему JSON-файлу
        String jsonFilePath = "/home/nikita/JavaProject/ParseSelenium/src" +
                "/main/java/org/example/SearchEngine/dictionary.json";
        // Создаем объект Gson
        Gson gson = new Gson();
        // Чтение JSON-файла
        Reader reader = new FileReader(jsonFilePath);
        // Парсинг JSON
        JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);

        JsonArray wordlist = jsonObject.getAsJsonArray("wordlist");

        for (int i = 0; i < wordlist.size(); i++) {
            JsonObject wordObject = wordlist.get(i).getAsJsonObject();

            int id = wordObject.get("id").getAsInt();
            String name = wordObject.get("name").getAsString().toLowerCase();

            // Если есть синонимы
            if (wordObject.has("synonyms")) {
                JsonArray synonyms = wordObject.getAsJsonArray("synonyms");
                List<String> synonymsList = new ArrayList<>();

                for (int j = 0; j < synonyms.size(); j++) {
                    String synonym = synonyms.get(j).getAsString().toLowerCase();
                    synonymsList.add(synonym);
                }
                try {
                    name = tokenize(name);
                }
                catch (Exception ex){
//                   System.out.println(ex.getMessage());
                }
                if (synonymMap.containsKey(name)) {
                    for(var synonym : synonymsList)
                        synonymMap.get(name).add(synonym);
                }
                else
                    synonymMap.put(name, synonymsList);
            }
        }
        System.out.println("Синонимы спаршены");
    }

    public String addSynonyms(String inputStr) throws ParseException {
        String strWithSynonyms = inputStr + " ";

        List<String> words = List.of(inputStr.split(" "));
        for(int i=0;i<words.size();i++){
            String word=words.get(i);
            try {
                word = tokenize(word);
            }
            catch (Exception ex){
//                System.out.println(ex.getMessage());
            }

            List<String> synonymList = synonymMap.get(word);
            if (synonymList !=null && !synonymList.isEmpty())
                strWithSynonyms += synonymList.get(0) + " ";
        }

        return strWithSynonyms;
    }

    public String tokenize(String str) throws ParseException {
        String query = new QueryParser("", analyzer)
                .parse(str).toString();
        return query.toString();
    }

}
