package samples;
public class TwoReturnOneConflict {
    public static int main(){
        int x = 0;
        if (x==1){ //source
            return 1;
        }
        return x; //sink
    }
}