package samples;
public class OneReturnZeroConflict {
    public static int main(){
        int x = 0;
        if (x==1){ //source
            x = 1;
        }
        return x; //sink
    }
}