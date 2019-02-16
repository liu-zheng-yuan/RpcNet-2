import java.util.ArrayList;
import java.util.List;

public class FibServiceImpl implements FibService{
    private List<Long> fibs = new ArrayList<>();

    {
        fibs.add(1L); // fib(0) = 1
        fibs.add(1L); // fib(1) = 1
    }
    @Override
    public long fib(int n) {
        for (int i = fibs.size(); i < n + 1; i++) {
            long value = fibs.get(i - 2) + fibs.get(i - 1);
            fibs.add(value);
        }
        return fibs.get(n);
    }

//    @Override
//    public String toString() {
//        return "FibServiceImpl{" +
//                "fibs=" + fibs +
//                '}';
//    }
}
