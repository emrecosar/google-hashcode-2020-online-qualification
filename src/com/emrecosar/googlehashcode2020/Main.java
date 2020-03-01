package com.emrecosar.googlehashcode2020;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

public class Main {

    // global variables;
    static int[] scores;
    static HashSet<Integer> set = new HashSet<>();
    static int totalBooks;
    static int totalLibrary;
    static int totalDayToProcess;
    static ArrayList<Library> libraries = new ArrayList<>();

    static String fileName;

    public static void main(String[] args) throws IOException {

        List<String> files = collectFiles();

        for (String file : files) {

            fileName = file;

            String input = readFile();

            fillObjects(input);

            signUpAndShipBooks();

            String[] outputs = prepareOutput();

            writeToFile(outputs);
        }

    }

    private static List<String> collectFiles() {
        List<String> files = new ArrayList<>();
        files.add("a_example.txt");
        //files.add("b_read_on.txt");
        //files.add("c_incunabula.txt");
        //files.add("d_tough_choices.txt");
        //files.add("e_so_many_books.txt");
        files.add("f_libraries_of_the_world.txt");
        return files;
    }

    private static void signUpAndShipBooks() {
        LocalDateTime start = LocalDateTime.now(ZoneOffset.UTC);

        // sort libraries as initial process
        sortLibraries();

        for (int d = 1; d <= totalDayToProcess; d++) {
            int signUpItem = -1;
            for (int l = 0; l < libraries.size(); l++) {
                Library library = libraries.get(l);
                if (signUpItem == -1 && library.daysInProgress < library.signUpDays) {
                    signUpItem = l;
                    library.daysInProgress++;
                } else {
                    if (library.daysInProgress >= library.signUpDays && !library.booksToShip.isEmpty()) {
                        library.shipBooks();
                        library.daysInProgress++;
                    }
                }
            }
            // sort libraries after per day
            sortLibraries();
        }
        LocalDateTime end = LocalDateTime.now(ZoneOffset.UTC);
        System.out.println(String.format("Took %sms for file : %s", ChronoUnit.MILLIS.between(start, end), fileName));
    }

    private static void sortLibraries() {
        libraries.sort((l, r) -> {
            if (l.calculateWeight() > r.calculateWeight())
                return -1;
            else if (l.calculateWeight() < r.calculateWeight())
                return 1;
            else return 0;
        });
    }

    private static class Library {

        int index;
        int signUpDays;
        List<Integer> booksToShip;
        List<Integer> shippedBooks;
        int maxShipPerDay;
        int totalScore;
        int daysInProgress;
        boolean needCalculation;

        public Library(int index, int signUpDays, List<Integer> books, int maxShipPerDay) {
            this.index = index;
            this.signUpDays = signUpDays;
            this.booksToShip = books;
            this.shippedBooks = new ArrayList<Integer>();
            this.maxShipPerDay = maxShipPerDay;
            this.daysInProgress = 0;
            this.needCalculation = true;
            calculateTotalScore();
        }

        private void sortBooksBasedOnScore() {
            booksToShip.sort((b1, b2) -> {
                if (!set.contains(b1) && !set.contains(b2))
                    return Integer.compare(scores[b1], scores[b2]);
                else if (set.contains(b1) && set.contains(b2))
                    return 0;
                else if (set.contains(b1))
                    return -1;
                else
                    return 1;
            });
        }

        private int calculateTotalScore() {
            sortBooksBasedOnScore();
            totalScore = 0;
            for (int i = 0; i < signUpDays * maxShipPerDay && i < booksToShip.size(); i++) {
                totalScore += scores[booksToShip.get(i)];
            }
            return totalScore;
        }

        public double calculateWeight() {
            needCalculation = false;
            return Double.valueOf(totalScore / signUpDays);
        }

        public void shipBooks() {
            sortBooksBasedOnScore();
            for (int i = 0; i < maxShipPerDay && i < booksToShip.size(); i++) {
                Integer book = booksToShip.remove(0);
                set.add(book);
                shippedBooks.add(book);
            }
            calculateTotalScore();
        }

    }

    private static void fillObjects(String input) {
        String[] lines = input.split("\\n");
        String[] mainParts = lines[0].split(" ");
        totalBooks = Integer.valueOf(mainParts[0]);
        totalLibrary = Integer.valueOf(mainParts[1]);
        totalDayToProcess = Integer.valueOf(mainParts[2]);

        // input read
        String[] scoresStr = lines[1].split(" ");
        scores = new int[scoresStr.length];
        for (int i = 0; i < scoresStr.length; i++) {
            scores[i] = Integer.valueOf(scoresStr[i]);
        }

        libraries.clear();
        set.clear();

        for (int i = 0; i < totalLibrary * 2; i = i + 2) {

            String[] libraryAttr = lines[i + 2].split(" ");
            int numOfBooks = Integer.valueOf(libraryAttr[0]);
            int signUpdays = Integer.valueOf(libraryAttr[1]);
            int shipPerDay = Integer.valueOf(libraryAttr[2]);

            String[] booksLine = lines[i + 3].split(" ");
            List<Integer> books = new ArrayList<Integer>();
            for (int b = 0; b < numOfBooks; b++) {
                books.add(Integer.valueOf(booksLine[b]));
            }
            Library library = new Library(i / 2 , signUpdays, books, shipPerDay);
            libraries.add(library);
        }
    }

    private static String readFile() {
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines(Paths.get("resource/" + fileName), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }

    private static void writeToFile(String[] input) throws IOException {

        File fout = new File("output/" + fileName);
        FileOutputStream fos = new FileOutputStream(fout);

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

        for (int i = 0; i < input.length; i++) {
            bw.write(input[i]);
            bw.newLine();
        }

        bw.close();
    }

    private static String[] prepareOutput() {
        String result = "";
        int totalSignedUpLibraryNumber = 0;
        for (int i = 0; i < libraries.size(); i++) {
            Library library = libraries.get(i);
            StringBuilder sb = new StringBuilder();
            if (library.daysInProgress > library.signUpDays) {
                totalSignedUpLibraryNumber++;
                sb.append(library.index).append(" ").append(library.shippedBooks.size()).append("\n");
                for (int b = 0; b < library.shippedBooks.size(); b++) {
                    sb.append(library.shippedBooks.get(b)).append(" ");
                }
                sb.append("\n");
                result += sb.toString();
            }
        }
        result = totalSignedUpLibraryNumber + " \n" + result;
        return result.split("\\n");
    }


}
