package week_1;



class Student{
    private int id; //fixed
    private String phoneNumber; //optional
    private String address; //optional
    private String email;   //optional

    public Student(int id){
        this.id = id;
    }

    public Student setPhoneNumber(String phoneNumber){
        this.phoneNumber = phoneNumber;
        return this;
    }
    public Student setEmail(String email){
        this.email = email;
        return this;
    }
    void print(){
        System.out.println(id +" -- " + " " + phoneNumber +" " + address + " " + email);
    }
}



public class Student_builder {
    public static void main(String[] args) {
        Student student = new Student(100).setPhoneNumber("12342");
        student.print();


    }
}


