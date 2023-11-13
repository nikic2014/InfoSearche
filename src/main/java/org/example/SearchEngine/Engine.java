package org.example.SearchEngine;

import com.google.gson.*;
import com.google.gson.internal.LinkedTreeMap;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.apache.lucene.analysis.synonym.SynonymGraphFilterFactory;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.Token;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.analysis.ru.RussianAnalyzer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.lucene.util.CharsRef;
import org.example.Parser.Record;

public class Engine {
    private class Pair {
        Object a;
        Object b;

        public Pair(Object a, Object b){
            this.a=a;
            this.b=b;
        }
    }

    private Directory memoryIndex;
    private RussianAnalyzer analyzer;
    private IndexWriterConfig indexWriterConfig;
    private IndexWriter writer;
    private Document document;
    public SynonymFilter synonymFilter;
    public Engine() throws IOException, ParseException {
        memoryIndex = new RAMDirectory();
        analyzer = new RussianAnalyzer();
        indexWriterConfig = new IndexWriterConfig(analyzer);
        writer = new IndexWriter(memoryIndex, indexWriterConfig);
        synonymFilter = new SynonymFilter(analyzer);
    }

    public void addRecord(Record record) throws IOException {
        this.document = new Document();
        this.document.add(new TextField("title", record.getTitle(),
                Field.Store.YES));
        this.document.add(new TextField("body", record.getText(),
                Field.Store.YES));

        writer.addDocument(this.document);
    }

    public void closeWriter() throws IOException {
        writer.close();
    }

    public List<Document> searchIndex(String inField, String queryString) throws ParseException, IOException {
        queryString = correctEngToRus(queryString);
        queryString = correctRussianTypos(queryString);
        queryString = synonymFilter.addSynonyms(queryString);
        Query query = new QueryParser(inField, analyzer)
                .parse(queryString);
//        System.out.println(query.toString());

        IndexReader indexReader = DirectoryReader.open(memoryIndex);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = searcher.search(query, 10);
        List<Document> documents = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            documents.add(searcher.doc(scoreDoc.doc));
        }

        return documents;
    }

    public String fixKeyboardLayout(String text) throws IOException {
        URL url = new URL("https://raskladki.net.ru/post.php");

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");

        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        // Включаем вывод данных
        connection.setDoOutput(true);
        connection.setDoInput(true);

        String formData = "text="+text+"&lang=eng2rus";


        // Получаем OutputStream для записи данных на сервер
        try (OutputStream os = connection.getOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            writer.write(formData);
        }


        int responseCode = connection.getResponseCode();

        // Если код ответа равен 200 (OK), то запрос успешно выполнен
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return response.toString();
        }
        else {
            System.out.println("Ошибка: " + responseCode);
        }

        return text;
    }

    public String correctEngToRus(String inputStr) throws IOException {
        int cnt=0;
        for(int i=0;i<inputStr.length();i++){
            if (inputStr.charAt(i)>='a' && inputStr.charAt(i)<='z')
                cnt++;
        }

        if (inputStr.length()<=cnt*2)
            return fixKeyboardLayout(inputStr);
        else
            return inputStr;
    }
    public String correctRussianTypos(String text) {
            try {
                // Encode the text for the URL
                String encodedText = URLEncoder.encode(text, "UTF-8");

                // Send a GET request to Yandex.Speller API for correction
                URL url = new URL("http://speller.yandex" +
                        ".net/services/spellservice.json/checkText?text="
                        + encodedText +
                        "&callback=fix_spell");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                // Check the HTTP response code
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    // Read the response from the API
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    int size = response.toString().length();
                    // Parse the JSON response using Gson
                    String substrResponse = response.substring(10, size - 1);
                    List<Pair> corrections = parseJsonResponse(substrResponse);

                    for (Pair correction : corrections) {
                        text = text.replace((String) correction.a,
                                            (String)correction.b);
                    }

                    return text;
                } else {
                    // Handle the error, e.g., by returning the original text
                    System.err.println("Error: Received HTTP response code " + responseCode);
                    return text;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return text; // Return the original text in case of an error
            }
    }

    private List<Pair> parseJsonResponse(String jsonResponse) {
        List<Pair> corrections = new ArrayList<>();
        JsonArray jsonArray = JsonParser.parseString(jsonResponse).getAsJsonArray();

        for (JsonElement element : jsonArray) {
            JsonObject jsonObject = element.getAsJsonObject();
            String word = jsonObject.get("word").getAsString();
            JsonArray suggestions = jsonObject.getAsJsonArray("s");
            for (JsonElement suggestion : suggestions) {
                corrections.add(new Pair(word, suggestion.getAsString()));
            }
        }
        return corrections;
    }
}

//public class SynonymAnalyzer extends Analyzer {
//
//    private final SynonymMap synonymMap;
//
//    public SynonymAnalyzer() throws IOException {
//        // Создайте карту синонимов
//        Map<String, String> synonymMappings = new HashMap<>();
//        synonymMappings.put("happy", "joyful");
//        synonymMappings.put("sad", "unhappy");
//
//        // Инициализируйте объект SynonymMap
//        this.synonymMap = buildSynonymMap(synonymMappings);
//    }
//
//    private SynonymMap buildSynonymMap(Map<String, String> synonymMappings) throws IOException {
//        SynonymMap.Builder builder = new SynonymMap.Builder(true);
//
//        for (Map.Entry<String, String> entry : synonymMappings.entrySet()) {
//            CharsRef input = new CharsRef(entry.getKey());
//            CharsRef output = new CharsRef(entry.getValue());
//            builder.add(input, output, true);
//        }
//
//        return builder.build();
//    }
//
//    @Override
//    protected TokenStreamComponents createComponents(String fieldName) {
//        TokenizerFactory tokenizerFactory = new WhitespaceTokenizerFactory(new HashMap<>());
//        TokenStream tokenStream = tokenizerFactory.create();
//
//        // Добавьте фильтры, включая SynonymGraphFilter
//        tokenStream = new LowerCaseFilterFactory(new HashMap<>()).create(tokenStream);
//        tokenStream = new SynonymGraphFilter(tokenStream, synonymMap, true);
//
//        // Важно вызывать reset() перед использованием TokenStream
//        try {
//            tokenStream.reset();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        TokenStream finalTokenStream = tokenStream;
//        return new TokenStreamComponents(tokenizerFactory.create(), finalTokenStream) {
//            @Override
//            protected void setReader(final Reader reader) {
//                super.setReader(reader);
//                // Важно вызывать reset() перед использованием TokenStream
//                try {
//                    finalTokenStream.reset();
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//
//            @Override
//            protected void finalize() throws Throwable {
//                try {
//                    // Важно вызывать close() перед завершением объекта TokenStream
//                    finalTokenStream.close();
//                } finally {
//                    super.finalize();
//                }
//            }
//        };
//    }
//}
//
//    public void test() throws IOException {
//        String text = "I am happy";
//        try (Analyzer analyzer = new SynonymAnalyzer()) {
//            try (TokenStream stream = analyzer.tokenStream("", new StringReader(text))) {
//                stream.reset();
//                while (stream.incrementToken()) {
//                    System.out.println(stream.getAttribute(org.apache.lucene.analysis.tokenattributes.CharTermAttribute.class));
//                }
//            }
//        }
//    }