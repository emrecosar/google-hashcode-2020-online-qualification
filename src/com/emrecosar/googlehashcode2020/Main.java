package com.emrecosar.googlehashcode2020;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    static HashSet<Integer> uniqueBooks = new HashSet<>();
    static int totalBooks;
    static int totalLibrary;
    static int totalDayToProcess;
    static int currentDayInProcess;
    static ArrayList<Library> libraries = new ArrayList<>();
    static String activeFile;
    static int filePoints;
    static int totalPoints;
    static long totalTimeInMilliSeconds;

    public static void main(String[] args) throws IOException {
        System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - ");
        System.out.println("GOOGLE HASHCODE 2020 ONLINE QUALIFICATION STATEMENT ");
        System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - ");
        System.out.println("   SCORE |                         FILE |   TIME(ms)");
        System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - ");

        List<String> files = collectFiles();

        for (String file : files) {

            currentDayInProcess = 0;
            filePoints = 0;
            activeFile = file;

            String input = readFile();

            fillObjects(input);

            signUpAndShipBooks();

            orderLibrariesBasedOnNumberOfInProgressDays();

            String[] outputs = prepareOutput();

            writeToFile(outputs);
            totalPoints += filePoints;
        }
        System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - ");
        System.out.println(String.format("%8d   %28s   %10s", totalPoints, "-- TOTAL --", totalTimeInMilliSeconds));
    }

    private static void orderLibrariesBasedOnNumberOfInProgressDays() {
        libraries.sort((l, r) -> Integer.compare(r.daysInProgress, l.daysInProgress));
    }

    private static List<String> collectFiles() {
        List<String> files = new ArrayList<>();
        files.add("a_example.txt");
        files.add("b_read_on.txt");
        files.add("c_incunabula.txt");
        files.add("d_tough_choices.txt");
        files.add("e_so_many_books.txt");
        files.add("f_libraries_of_the_world.txt");
        return files;
    }

    private static void signUpAndShipBooks() {
        LocalDateTime start = LocalDateTime.now(ZoneOffset.UTC);

        // sort libraries as initial process
        sortLibraries();

        Library libraryInProgress = null;
        for (currentDayInProcess = 1; currentDayInProcess <= totalDayToProcess; currentDayInProcess++) {
            int signUpItem = -1;
            for (int l = 0; l < libraries.size(); l++) {
                Library library = libraries.get(l);
                if (signUpItem == -1 && library.daysInProgress < library.signUpDays) {
                    signUpItem = l;
                    libraryInProgress = library;
                    library.incrementDaysInProgress(); // sign up in progress
                } else {
                    if (library.daysInProgress >= library.signUpDays) {
                        library.shipBooks(); // ship books if any
                    }
                }
            }
            // sort libraries after per signup process ends
            if (libraryInProgress.daysInProgress == libraryInProgress.signUpDays)
                sortLibraries();
        }
        long interval = ChronoUnit.MILLIS.between(start, LocalDateTime.now(ZoneOffset.UTC));
        totalTimeInMilliSeconds += interval;
        System.out.println(String.format("%8d | %28s | %10s", filePoints, activeFile, interval));
    }

    private static void sortLibraries() {
        // calculate weight again per library
        libraries.parallelStream().forEach( l -> l.calculateTotalScoreAndWeight());
        // sort library based on their weight descending
        libraries.sort((l, r) -> {
            if (l.weight > r.weight)
                return -1;
            else if (l.weight < r.weight)
                return 1;
            else {
                // sort library based on their signUpDays ascending
                if (l.signUpDays > r.signUpDays)
                    return 1;
                else if (l.signUpDays < r.signUpDays)
                    return -1;
                else {

                        // sort library based on their maxShipPerDay descending
                        if (l.maxShipPerDay > r.maxShipPerDay)
                            return -1;
                        else if (l.maxShipPerDay < r.maxShipPerDay)
                            return 1;
                        else {
                            // sort library based on their booksToShip descending
                            if (l.booksToShip.size() > r.booksToShip.size())
                                return -1;
                            else if (l.booksToShip.size() < r.booksToShip.size())
                                return 1;
                            else {
                                return 0;
                                /*
                                if (l.totalUniqueBooks.size() - l.uniqueShippedBooks.size() < r.totalUniqueBooks.size() - r.uniqueShippedBooks.size())
                                    return 1;
                                else if (l.totalUniqueBooks.size() - l.uniqueShippedBooks.size() > r.totalUniqueBooks.size() - r.uniqueShippedBooks.size())
                                    return -1;
                                else {
                                    return 0;
                                }
                                */
                            }
                        }

                }
            }
        });
    }

    private static class Library {

        int index;
        List<Integer> booksToShip;
        List<Integer> shippedBooks;
        HashSet<Integer> uniqueShippedBooks;
        HashSet<Integer> totalUniqueBooks;
        int signUpDays;
        int maxShipPerDay;
        int daysInProgress;
        int totalScore;
        double weight;

        public Library(int index, int signUpDays, List<Integer> books, int maxShipPerDay) {
            this.index = index;
            this.signUpDays = signUpDays;
            this.booksToShip = books;
            this.shippedBooks = new ArrayList<Integer>();
            this.uniqueShippedBooks = new HashSet<>();
            this.totalUniqueBooks = new HashSet<>();
            this.maxShipPerDay = maxShipPerDay;
            this.daysInProgress = 0;
            calculateTotalScoreAndWeight();
            fillUniqueShippedBooks();
        }

        public void fillUniqueShippedBooks() {
            for(Integer book : booksToShip) {
                uniqueShippedBooks.add(book);
            }
        }

        /**
         * At first, need sorting for all
         * Later, only pick valuable ones and then put in the beginning
         */
        private void sortBooksToShipBasedOnScore() {
            booksToShip.sort((book1, book2) -> {
                if (!uniqueBooks.contains(book1) && !uniqueBooks.contains(book2))
                    return Integer.compare(scores[book2], scores[book1]);
                else if (uniqueBooks.contains(book1) && uniqueBooks.contains(book2))
                    return 0;
                else if (uniqueBooks.contains(book1))
                    return 1;
                else
                    return -1;
            });
        }

        private int calculateTotalScoreAndWeight() {
            sortBooksToShipBasedOnScore();
            totalScore = 0;
            int signUpRemainingDays = daysInProgress >= signUpDays ? 0 : ( signUpDays >= totalDayToProcess - currentDayInProcess ? 0 : signUpDays );
            for (int i = 0; i < signUpRemainingDays * maxShipPerDay && i < booksToShip.size(); i++) {
                totalScore += scores[booksToShip.get(i)];
            }
            weight = Double.valueOf(totalScore / signUpDays);
            return totalScore;
        }

        public void shipBooks() {
            sortBooksToShipBasedOnScore();
            for (int i = 0; i < maxShipPerDay && i < booksToShip.size(); i++) {
                Integer book = booksToShip.remove(0);
                if (!uniqueBooks.contains(book)) {
                    filePoints += scores[book];
                    uniqueBooks.add(book);
                }
                uniqueShippedBooks.add(book);
                shippedBooks.add(book);
            }
            incrementDaysInProgress();
        }

        public void incrementDaysInProgress() {
            daysInProgress++;
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
        uniqueBooks.clear();

        for (int i = 0; i < totalLibrary * 2; i = i + 2) {

            String[] libraryAttr = lines[i + 2].split(" ");
            int numOfBooks = Integer.valueOf(libraryAttr[0]);
            int signUpdays = Integer.valueOf(libraryAttr[1]);
            int shipPerDay = Integer.valueOf(libraryAttr[2]);

            if(signUpdays > totalDayToProcess)
                continue;

            String[] booksLine = lines[i + 3].split(" ");
            List<Integer> books = new ArrayList<>();
            HashSet<Integer> uniqueBooks = new HashSet<>();
            for (int b = 0; b < numOfBooks; b++) {
                Integer book = Integer.valueOf(booksLine[b]);
                books.add(book);
                if (!uniqueBooks.contains(book)) {
                    uniqueBooks.add(book);
                }
            }
            Library library = new Library(i / 2, signUpdays, books, shipPerDay);
            libraries.add(library);
        }
    }

    private static String readFile() {
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines(Paths.get("resource/" + activeFile), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }

    private static void writeToFile(String[] input) throws IOException {

        File fout = new File("output/" + activeFile);
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
