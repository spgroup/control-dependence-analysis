package samples;
public class BlackBoard {
    public static String text = "";
    public static void main(){
        if (text != "" && hasWhiteSpace()){
            normalizeWhiteSpace();
            removeDuplicateWords();
        }
    }

    public static void normalizeWhiteSpace(){};
    public static boolean hasWhiteSpace(){
        return true;
    };
    public static void removeDuplicateWords(){};

}