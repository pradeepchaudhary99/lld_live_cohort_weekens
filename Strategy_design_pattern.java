

interface ISorting{
    void sort(int[]arr);
}

class MS implements ISorting{
    @Override
    public void sort(int[] arr) {
        System.out.println("Mergesort");
    }
}

class QS implements ISorting{
    @Override
    public void sort(int[] arr) {
        System.out.println("QuickSort");
    }
}

class SortingService{
    ISorting sortingStrategy;
    public SortingService(ISorting sortingStrategy){
        this.sortingStrategy = sortingStrategy;
    }

    void sort(int [] arr){
        sortingStrategy.sort(arr);
    }

    void changeStrategy(ISorting sortingStrategy){
        this.sortingStrategy = sortingStrategy;
    }
}


public class Strategy_design_pattern {
    public static void main(String[] args) {
        SortingService service = new SortingService(new MS());
        int [] arr = {1,2};

        service.changeStrategy(new QS());
        service.sort(arr);
    }
}
