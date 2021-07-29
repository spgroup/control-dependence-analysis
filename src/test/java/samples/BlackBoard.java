package samples;
public class BlackBoard {
    public static int main(){
        int x = 0;
        if (x == 1){
            x = x + 1;
        }

        while (x ==2){
            x = x + 2;
            x = x + 3;
        }
        if (x != 0){
            x = x * 2;
        }
        x = x + 4;
        return x+1;
    }


}