package samples;
public class BlackBoard {
    public static int main(){
        int x = 0;
        if (x==0){
            x = x + 1;
            return 0;
        }
        if (x == 1){
            x = x + 2;
            return 1;
        }else{
            x = x + 3;
        }
        while (x == 2){
            x = x + 4;
            return 2;
        }
        return 3;
    }


}