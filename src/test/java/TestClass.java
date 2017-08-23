import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.vavr.Function1;

import static java.util.stream.Collectors.toList;

public class TestClass {

    int globalValue = 0;

    public List<String> imperativ (String fileName) throws IOException {
        List<String> errors = new ArrayList<>();
        int errorCount = 0;
        FileReader fr = new FileReader(fileName);
        BufferedReader br = new BufferedReader(fr);
        String line = br.readLine();
        while (errorCount < 40 && line != null){
            if(line.startsWith("Error")){
                errors.add(line);
                errorCount++;
            }
            line = br.readLine();
        }
        return errors;
    }

    public  List<String> functional (String fileName) throws IOException {
        List<String> errors = Files.lines(Paths.get(fileName))
                .filter(l -> l.startsWith("ERROR"))
                .limit(40).collect(toList());
        return errors;
    }

    public int mitSeitenEffekt(int x) {
        if(globalValue == 10) throw new IllegalStateException("Invalid parameter");
        globalValue++;
        return x + globalValue;
    }

    int add(int x, int y) {
        return x + y;
    }


    public Function1<Integer, Integer> multiply(Function<Integer, Integer> function) {
       return (y) -> function.apply(y) * y;
    }


    public List<String> filterOranges(List<String> fruits){
        return fruits.stream()
                .map(String::toUpperCase)
                .filter(fruit -> fruit.equals("ORAGNE"))
                .collect(Collectors.toList());
    }

    List<User> users = new ArrayList<>();
    public void addUser(User user){
        validateUser(user);
        users.add(user);
    }

    private void validateUser(User user) {

    }
}
