package org.example.Parser;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Selenium {

    private List<Record> records;
    private final Gson gson;

    public Selenium(){
        this.gson=new Gson();
        this.records=new ArrayList<>();
    }

    public boolean hasRecords(Record record){
        for(int i=0;i<this.records.size();i++) {
            Record recordInList = this.records.get(i);
            if (recordInList.getTitle().equals(record.getTitle()))
                return true;
        }
        return false;
    }

    public void parse() throws IOException {
//        ChromeOptions options = new ChromeOptions();
//        options.setHeadless(true);
//        WebDriver driver = new ChromeDriver(options);
        WebDriver driver = new ChromeDriver();
        driver.get("https://www.film.ru/topic/news");
        driver.manage().window().maximize();
        WebElement main_topic_list = driver.findElement(By.className(
                "redesign_topic_list"));

        JavascriptExecutor js = (JavascriptExecutor) driver;
        int scrollX = 200;
        while (true) {
            js.executeScript("window.scroll(0," + scrollX +")");
            scrollX += 25;

//            System.out.println(scrollX);
            if (scrollX==500000)
                break;
        }
        List<WebElement> skins = main_topic_list.findElements(By.className(
                "skin"));

        List<String> topicList = new ArrayList<>();
        for(int i=0;i<skins.size();i++) {
            WebElement skin = skins.get(i);
            List<WebElement> redesign_topic_List =
                    skin.findElements(By.className(
                    "redesign_topic"));
            for(var j:redesign_topic_List){
                String link = j.getAttribute("onclick");
                link = link.substring(15);
                link = "https://www.film.ru" + link.substring(0,
                    link.length() - 2);
                topicList.add(link);
            }
        }

//        FileWriter fileWriter = new FileWriter("topick list");
//        for(var i:topicList) {
//            fileWriter.write(i);
//            fileWriter.append('\n');
//        }
//        System.out.println(topicList.size());
//        fileWriter.close();

//        List<String> topicList;
//        String str = Files.readString(Path.of("topick list"));
//        topicList = List.of(str.split("\n"));

        for (var i:topicList){
//            String link = topicList.get(i);
            try {
                driver.get(i);

                String title = driver.findElement(By.tagName("h1")).getText();
                String body = "";
                WebElement divText = driver.findElement(By.className(
                    "wrapper_articles_text"));
                List<WebElement> p = divText.findElements(By.tagName("p"));
                for (int k = 0; k < p.size(); k++) {
                    body += p.get(k).getText();
                }
                Record record = new Record(title,
                    body,
                    "test");

                if (!this.hasRecords(record))
                    this.records.add(record);

                    System.out.println("Добавлена запись" + i + ": " + title);
                }
            catch (Exception ex){
                System.out.println(ex.getMessage());
            }
        }
        driver.close();

//        for(int i=0;i<skins.size();i++){
//            WebElement skin = skins.get(i);
//            List<WebElement> topicList = skin.findElements(By.className(
//                    "redesign_topic"));
//            for (int j=0;j<topicList.size();j++) {
//                WebElement now = topicList.get(j);
//                System.out.println();
//                System.out.println("Skin " + i + "redesign_topic" + j);
//                System.out.println(now.getText());
//            }
//
//            for (int j=0;j<topicList.size();j++) {
//                try {
//                    WebElement now = topicList.get(j);
//                    System.out.println();
//                    System.out.println("Skin " + i + "redesign_topic" + j);
//                    System.out.println(now.getText());
//                    topicList.get(j).click();
//                    String title = driver.findElement(By.tagName("h1")).getText();
//                    String body = "";
//                    WebElement divText = driver.findElement(By.className(
//                            "wrapper_articles_text"));
//                    List<WebElement> p = divText.findElements(By.tagName("p"));
//                    for (int k = 0; k < p.size(); k++) {
//                        body += p.get(k).getText();
//                    }
//                    Record record = new Record(title,
//                            body,
//                            "test");
//
//                    if (!this.hasRecords(record))
//                        this.records.add(record);
//
//                    driver.navigate().back();
//                }
//                catch (Exception ex){
//                    System.out.println(ex.getMessage());
//                }
//            }
//        }
    }

    public List<Record> getRecords(){
        return this.records;
    }

    public void writeToJson() throws IOException {
        String json = this.gson.toJson(this.records);
        FileWriter out = new FileWriter("src/main/java/org/example" +
                "/Parser/data.json");
        out.write(json);
        out.close();
    }
    public void parseJson() throws IOException {
       List<String> str = Files.readAllLines(Paths.get(
               "src/main/java/org/example" +
               "/Parser/data.json"));
       try {
           Type type = new TypeToken<List<Record>>(){}.getType();
           this.records = this.gson.fromJson(str.get(0), type);
       }
       catch (Exception ex){
           System.out.println("Json is empty");
       }

    }
}
