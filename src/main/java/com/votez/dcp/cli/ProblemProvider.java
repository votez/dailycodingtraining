package com.votez.dcp.cli;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

@Slf4j
@ShellComponent
public class ProblemProvider {
    private LocalDateTime started;
    private Iterator<Map.Entry<String, String>> easy;
    private Iterator<Map.Entry<String, String>> medium;
    private Iterator<Map.Entry<String, String>> hard;
    private Iterator<Map.Entry<String, String>> lastUsedDifficulty;
    private String currentHeader;
    private int counter = 0;

    @PostConstruct
    public void initProblems() throws IOException {
        var resolver = new PathMatchingResourcePatternResolver();
        var resources = resolver.getResources("classpath:dcp/*");
        var duplicates = new HashSet<String>();
        var easy = new HashMap<String, String>();
        var medium = new HashMap<String, String>();
        var hard = new HashMap<String, String>();
        var solved = Arrays.stream(Preferences.userNodeForPackage(ProblemProvider.class).get("solved", "")
                        .split(","))
                .filter(Predicate.not(x -> x.equals(",")))
                .filter(Predicate.not(String::isEmpty))
                .collect(Collectors.toSet());
        var skipped = Arrays.stream(Preferences.userNodeForPackage(ProblemProvider.class).get("skipped", "")
                        .split(","))
                .filter(Predicate.not(x -> x.equals(",")))
                .filter(Predicate.not(String::isEmpty))
                .collect(Collectors.toSet());

        log.info("You have solved {} problems", solved.size());
        log.info("You have skip {} problems", skipped.size());
        for (Resource resource : resources) {
            Map<String, String> target;
            if (skipped.contains(resource.getFilename().substring(0, 4))) {
                continue;
            }
            if (solved.contains(resource.getFilename().substring(0, 4))) {
                continue;
            }

            if (resource.getFilename().contains("Easy")) {
                target = easy;
            } else if (resource.getFilename().contains("Medium")) {
                target = medium;
            } else {
                target = hard;
            }
            String contents = Files.readString(Path.of(resource.getURI()));
            if(duplicates.contains(contents)){
                log.debug("Skip problem {} since it is a duplicate", resource.getFilename());
            } else {
                target.put(resource.getFilename().substring(0, 4), contents);
                duplicates.add(contents);
            }
        }
        List<Map.Entry<String, String>> easyEntries = new ArrayList<>(easy.entrySet());
        List<Map.Entry<String, String>> mediumEntries = new ArrayList<>(medium.entrySet());
        List<Map.Entry<String, String>> hardEntries = new ArrayList<>(hard.entrySet());
        Collections.shuffle(easyEntries);
        Collections.shuffle(mediumEntries);
        Collections.shuffle(hardEntries);
        log.info("Prepared {} easy problems", easyEntries.size());
        log.info("Prepared {} medium problems", mediumEntries.size());
        log.info("Prepared {} hard problems", hardEntries.size());
        this.easy = easyEntries.iterator();
        this.medium = mediumEntries.iterator();
        this.hard = hardEntries.iterator();
    }

    @PreDestroy
    public void save() throws BackingStoreException {
        Preferences.userNodeForPackage(ProblemProvider.class).flush();
        log.debug("Successfully saved progress");
        log.info("Solved {} problems", counter);
    }

    @ShellMethod(value = "provides next easy problem", key = {"next easy", "easy"})
    public void nextEasy() throws IOException {
        next(easy);
    }

    @ShellMethod(value = "provides next medium problem", key = {"next medium", "medium"})
    public void nextMedium() throws IOException {
        next(medium);
    }

    @ShellMethod(value = "provides next hard problem", key = {"next hard", "hard"})
    public void nextHard() throws IOException {
        next(medium);
    }

    @ShellMethod(value = "provides next problem of the same difficulty level", key = "next")
    public void next() throws IOException {
        next(lastUsedDifficulty);
    }


    public void next(Iterator<Map.Entry<String, String>> iterator) throws IOException {
        started = LocalDateTime.now();
        Map.Entry<String, String> problem = iterator.next();
        currentHeader = problem.getKey();
        log.info("Starting problem {}, which is {} of today", currentHeader, counter + 1);
        log.info("\n\nProblem #{}\n\n{}", currentHeader, problem.getValue());
        lastUsedDifficulty = iterator;
    }

    @ShellMethod(value = "mark current as done", key = "done")
    public void done() {
        log.info("It took " + ChronoUnit.MINUTES.between(started, LocalDateTime.now()) + " minutes");
        started = null;
        Preferences preferences = Preferences.userNodeForPackage(ProblemProvider.class);
        var solved = preferences.get("solved", "");
        solved += "," + currentHeader;
        preferences.put("solved", solved);
        counter++;
    }

    @ShellMethod(value = "skip current", key = "skip")
    public void skip() {
        log.info("You gave up after " + ChronoUnit.MINUTES.between(started, LocalDateTime.now()) + " minutes");
        started = null;
        Preferences preferences = Preferences.userNodeForPackage(ProblemProvider.class);
        var skipped = preferences.get("skipped", "");
        skipped += "," + currentHeader;
        preferences.put("skipped", skipped);
    }

    @ShellMethod(value = "reset solved database", key = "reset solved database")
    public void resetDb() {
        Preferences.userNodeForPackage(ProblemProvider.class).put("skipped", "");
        Preferences.userNodeForPackage(ProblemProvider.class).put("solved", "");
    }

    public Availability doneAvailability() {
        return started == null ? Availability.unavailable("no problem has started yet") : Availability.available();
    }

    public Availability skipAvailability() {
        return started == null ? Availability.unavailable("no problem has started yet") : Availability.available();
    }

    public Availability laterAvailability() {
        return started == null ? Availability.unavailable("no problem has started yet") : Availability.available();
    }

    public Availability nextAvailability() {
        return lastUsedDifficulty != null && lastUsedDifficulty.hasNext() ? Availability.available() : Availability.unavailable("no problem has started yet");
    }

    public Availability nextEasyAvailability() {
        return easy.hasNext() ? Availability.available() : Availability.unavailable("no more problems available");
    }

    public Availability nextMediumAvailability() {
        return medium.hasNext() ? Availability.available() : Availability.unavailable("no more problems available");
    }

    public Availability nextHardAvailability() {
        return hard.hasNext() ? Availability.available() : Availability.unavailable("no more problems available");
    }
}
