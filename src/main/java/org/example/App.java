package org.example;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.example.Parser.Record;
import org.example.Parser.Selenium;
import org.example.SearchEngine.Engine;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class App 
{
    public static void main( String[] args ) throws InterruptedException, IOException, ParseException {
        Selenium selenium = new Selenium();
        selenium.parseJson();
//        selenium.parse();
//        selenium.writeToJson();
        List<Record> recordList = selenium.getRecords();
        System.out.println(recordList.size());

        Engine engine = new Engine();
        System.out.println(engine.synonymFilter.addSynonyms("арестант армия"));

        for(int i=0;i<recordList.size();i++){
            engine.addRecord(recordList.get(i));
        }
        System.out.println("End writing records");

        engine.closeWriter();
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String inField = scanner.next();
            String query = scanner.nextLine();

            System.out.println("Запрос:");
            System.out.println(inField + " " + query);

            if (inField.equals("stop"))
                break;

            System.out.println("Ответ:");
            List<Document> test = engine.searchIndex(inField, query);
            if (test.size() == 0)
                System.out.println("Ничего не найдено");
            else
                for (int i = 0; i < test.size(); i++) {
                    System.out.println(test.get(i).getFields().get(0));
//                    System.out.println(test.get(i).getFields().get(1));
                }
        }
    }
}
