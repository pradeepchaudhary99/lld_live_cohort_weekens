


class Student{
    private int id; //fixed
    private String phoneNumber; //optional
    private String address; //optional
    private String email;   //optional

    private Student(int id,String phoneNumber,String address,String email){
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.email = email;
    }
    void print(){
        System.out.println(id +" -- " + " " + phoneNumber +" " + address + " " + email);
    }
    static class StudentBuilder{
        private int id;
        private String phoneNumber; //optional
        private String address; //optional
        private String email;   //optional

        public StudentBuilder(int id){
            if(id < 0){
                return;
            }

        }

        public StudentBuilder setPhoneNumber(String number){
            this.phoneNumber = number;
            return this;
        }

        public StudentBuilder setAddress(String address){
            this.address = address;
            return this;
        }
        public StudentBuilder setEmail(String email){
            this.email = email;
            return this;
        }
        
        public Student build(){
            return new Student(id,phoneNumber,address,email);
        }
    }
}



public class Builder_design_pattern {
    public static void main(String[] args) {
        Student student = new Student.StudentBuilder(-100).setPhoneNumber("32123").setEmail("myEmail").build();
        student.print();
    }
}



// DB ---> Fetch the schema ----> create the class --> builder
