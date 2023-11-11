package com.votez.dcp.cli;

import org.simplejavamail.api.email.Email;
import org.simplejavamail.converter.EmailConverter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Converts *eml format files from DailyCodingProblem (i.e. use Thunderbird email client do download mails) into files
 * understood by the project.
 */
public class ConvertEml {
    /**
     *
     * @param args first arg is the source directory with eml, second arg is the destination for txt
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        if( !new File(args[0]).exists() ) {
            System.out.println("Source path does not exists: " + args[1]);
            System.exit(1);
        }
        if( !new File(args[1]).exists() ) {
            System.out.println("Destination path does not exists: " + args[1]);
            System.exit(1);
        }
        Files.list(Path.of(args[0]))
                .map(Path::toFile)
                .filter( file->file.getName().startsWith("Daily"))
                .forEach(
                path-> {
                    var numberStart = path.getName().indexOf('#');
                    var numberEnd = path.getName().indexOf(' ', numberStart);
                    var number = Integer.parseInt(path.getName().substring(numberStart+1, numberEnd));
                    var difficultyStart = path.getName().indexOf('[');
                    var difficultyEnd = path.getName().indexOf(']');
                    var difficulty = path.getName().substring(difficultyStart+1, difficultyEnd);

                    Email email = EmailConverter.emlToEmail(path);
                    var text = email.getPlainText();
                    var endText = text.indexOf("Upgrade to premium");
                    System.out.println("#" + number + "-" + difficulty + " : ");
                    System.out.println(text.substring(0, endText));
                    var decoded = new File(args[1] + File.separatorChar + String.format("%04d-%s.txt", number,difficulty));
                    try {
                        decoded.createNewFile();
                        var writer = new FileWriter(decoded);
                        writer.write(text.substring(0, endText));
                        writer.close();
                        System.out.println("File " + decoded + " written");
                    } catch (IOException e) {
                        System.out.println("File " + decoded + "  cannot be processed");
                        throw new RuntimeException(e);
                    }
                }
        );
    }
}
