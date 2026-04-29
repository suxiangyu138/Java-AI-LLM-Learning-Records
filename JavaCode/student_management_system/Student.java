package student_management_system;
public class Student implements Comparable<Student> {
    private Integer id;
    private String name;
    private Integer score;

    public Student(Integer id, String name, Integer score) {
        this.id = id;
        this.name = name;
        this.score = score;
    }

    // 重写equals + hashCode → 保证Set自定义对象去重
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Student student = (Student) o;
        return id.equals(student.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    // 自然排序：按学号id升序
    @Override
    public int compareTo(Student o) {
        return Integer.compare(this.id, o.id);
    }

    @Override
    public String toString() {
        return "Student{id=" + id + ", name='" + name + "', score=" + score + "}";
    }

    // Getter
    public Integer getId() { return id; }
    public Integer getScore() { return score; }
    public String getName() { return name; }
}