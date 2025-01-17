package com.github.chen0040.embeddings;

import com.github.chen0040.embeddings.utils.HttpClient;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GloVeModel {

    private static final String url = "https://nlp.stanford.edu/data/glove.6B.zip";
    private Map<String, float[]> word2em = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(GloVeModel.class);
    private int dimension = -1;

    public static List<Integer> getAvailableDimensionList() {
        return Arrays.asList(50, 100, 200, 300);
    }

    private static final String getGloVeTextFileName(int dimension){
        return "glove.6B." + dimension + "d.txt";
    }

    public Map<String, float[]> load100() {
        return load(100);
    }

    public Map<String, float[]> load50() {
        return load(50);
    }

    public Map<String, float[]> load200() {
        return load(200);
    }

    public Map<String, float[]> load300() {
        return load(300);
    }

    public Map<String, float[]> load(int dimension) {
        return load("/tmp", dimension);
    }

    public float[] encodeWord(String word) {
        word = word.toLowerCase();
        if(word2em.containsKey(word)) {
            return word2em.get(word);
        }
        return null;
    }

    public float[] encodeDocument(String sentence) {
        sentence = filter(sentence);
        String[] words = sentence.split(" ");

        float[] vec = new float[dimension];
        for(String word: words) {
            String w = word.trim();
            if(w.equals("")){
                continue;
            }
            float[] word2vec = encodeWord(w);
            if(word2vec == null) continue;
            for(int i=0; i < dimension; ++i){
                vec[i] += word2vec[i];
            }
        }

        return vec;

    }

    private String filter(String sent) {
        sent = sent.toLowerCase();
        String[] punctuations = new String[] {",", ".", ";", ":", "?", "!", "\"", "'"};
        for(String punt : punctuations) {
            sent = sent.replace(punt, " " + punt);
        }
        return sent;
    }

    public int size() {
        return word2em.size();
    }

    public int getWordVecDimension() {
        return dimension;
    }

    public Map<String, float[]> load(String dirPath, int dimension){
        this.dimension = -1;
        word2em.clear();
        String sourceFile100 = getGloVeTextFileName(dimension);
        String filePath = dirPath + "/" + sourceFile100;
        File file = new File(filePath);
        if(!file.exists()){

            String zipFilePath = dirPath + "/glove.6B.zip";
            if(!new File(zipFilePath).exists()) {
                logger.info("{} not found on local machine, downloading it from {}", zipFilePath, url);
                if (!HttpClient.downloadFile(url, zipFilePath)) {
                    return word2em;
                } else {
                    logger.info("{} is downloaded", zipFilePath);
                }
            }

            if(!unZip(zipFilePath, dirPath)){
                return word2em;
            }
        }

        logger.info("loading {} into word2em", filePath);

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath))))){
            String line;
            while((line=reader.readLine()) != null) {
                String[] parts = line.split(" ");
                String word = parts[0];
                float[] vec = new float[dimension];
                for(int i=1; i <= dimension; ++i) {
                    vec[i-1] = Float.parseFloat(parts[i]);
                }
                word2em.put(word, vec);
                
            }
        } catch (IOException e) {
            logger.error("Failed to read file " + filePath, e);
            word2em.clear();
            return new HashMap<>();
        }

        this.dimension = dimension;

        return word2em;

    }

    private boolean unZip(String zipFilePath, String dirPath) {
        logger.info("unzipping {} to {}", zipFilePath, dirPath);
        try {
            ZipFile zipFile = new ZipFile(zipFilePath);
            zipFile.extractAll(dirPath);
            return true;
        }
        catch (ZipException e) {
            logger.error("Failed to unzip " + zipFilePath, e);
            return false;
        }
    }

    public double distance(String word1, String word2) {
        float[] vec1 = encodeWord(word1);
        float[] vec2 = encodeWord(word2);

        if(vec1 == null || vec2 == null) {
            return -1f;
        }

        float result = 0;
        for(int i=0; i < dimension; ++i) {
            float v1 = vec1[i];
            float v2 = vec2[i];

            result += (v1 - v2) * (v1 - v2);
        }

        return Math.sqrt(result);
    }
}
