package samples;
public class OneDoWhileAndOneIf {
    public static void main() {
        int x = 1;
        do{
            x = 2;
            if (x == 0){
                x = 3; //source
            }
            x = 5;
        }while (x < 1); //sink
        x = 4;
    }
}