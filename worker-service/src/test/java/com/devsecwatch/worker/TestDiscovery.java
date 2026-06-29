import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestDiscovery {
    public static void main(String[] args) throws Exception {
        Path repoPath = Paths.get("s:/Project Folders/DevSecWatch/devsecwatch-frontend");
        System.out.println("Walking: " + repoPath);
        
        try (Stream<Path> paths = Files.walk(repoPath, 3)) {
            List<Path> manifestFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String pStr = p.toString();
                        boolean exclude = pStr.contains("node_modules") 
                            || pStr.contains(".git") 
                            || pStr.contains("target");
                        return !exclude;
                    })
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return "package.json".equals(name) || "pom.xml".equals(name) || "requirements.txt".equals(name);
                    })
                    .collect(Collectors.toList());

            System.out.println("Found " + manifestFiles.size() + " manifests:");
            for (Path m : manifestFiles) {
                System.out.println(" - " + m);
            }
        }
    }
}
