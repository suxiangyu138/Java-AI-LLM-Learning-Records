class Main {
    static class Student {
        String id;
        String name;
        double javaScore;
        double sqlScore;
        double englishScore;

        Student(String id, String name, double javaScore, double sqlScore, double englishScore) {
            this.id = id;
            this.name = name;
            this.javaScore = javaScore;
            this.sqlScore = sqlScore;
            this.englishScore = englishScore;
        }

        double total() {
            return javaScore + sqlScore + englishScore;
        }

        double avg() {
            return total() / 3.0;
        }

        public String toString() {
            return "学号：" + id +
                    "，姓名：" + name +
                    "，Java：" + javaScore +
                    "，MySQL：" + sqlScore +
                    "，英语：" + englishScore +
                    "，总分：" + String.format("%.2f", total()) +
                    "，平均分：" + String.format("%.2f", avg());
        }
    }

    static class StudentService {
        java.util.ArrayList<Student> list = new java.util.ArrayList<>();

        boolean add(Student s) {
            if (findById(s.id) != null) return false;
            list.add(s);
            return true;
        }

        Student findById(String id) {
            for (Student s : list) {
                if (s.id.equals(id)) return s;
            }
            return null;
        }

        boolean deleteById(String id) {
            Student s = findById(id);
            if (s == null) return false;
            list.remove(s);
            return true;
        }

        boolean update(String id, double javaScore, double sqlScore, double englishScore) {
            Student s = findById(id);
            if (s == null) return false;
            s.javaScore = javaScore;
            s.sqlScore = sqlScore;
            s.englishScore = englishScore;
            return true;
        }

        void showAll() {
            if (list.isEmpty()) {
                System.out.println("暂无学生数据");
                return;
            }
            for (Student s : list) {
                System.out.println(s);
            }
        }

        void sortByTotalDesc() {
            list.sort((a, b) -> Double.compare(b.total(), a.total()));
            System.out.println("已按总分降序排序");
        }

        void statistics() {
            if (list.isEmpty()) {
                System.out.println("暂无学生数据");
                return;
            }

            double sum = 0;
            double max = list.get(0).total();
            double min = list.get(0).total();
            Student maxStu = list.get(0);
            Student minStu = list.get(0);
            int passCount = 0;

            for (Student s : list) {
                double total = s.total();
                sum += total;
                if (total > max) {
                    max = total;
                    maxStu = s;
                }
                if (total < min) {
                    min = total;
                    minStu = s;
                }
                if (s.javaScore >= 60 && s.sqlScore >= 60 && s.englishScore >= 60) {
                    passCount++;
                }
            }

            System.out.println("学生总人数：" + list.size());
            System.out.println("总分平均值：" + String.format("%.2f", sum / list.size()));
            System.out.println("总分最高分：" + String.format("%.2f", max) + "，姓名：" + maxStu.name);
            System.out.println("总分最低分：" + String.format("%.2f", min) + "，姓名：" + minStu.name);
            System.out.println("三科全及格人数：" + passCount);
            System.out.println("三科全及格率：" + String.format("%.2f", passCount * 100.0 / list.size()) + "%");
        }
    }

    public static void main(String[] args) {
        java.util.Scanner sc = new java.util.Scanner(System.in);
        StudentService service = new StudentService();

        while (true) {
            System.out.println("\n====== 学生成绩管理系统 ======");
            System.out.println("1. 添加学生");
            System.out.println("2. 显示所有学生");
            System.out.println("3. 按学号查询");
            System.out.println("4. 修改成绩");
            System.out.println("5. 删除学生");
            System.out.println("6. 按总分降序排序");
            System.out.println("7. 统计分析");
            System.out.println("0. 退出系统");
            System.out.print("请选择：");

            String choice = sc.next();

            switch (choice) {
                case "1":
                    System.out.print("请输入学号：");
                    String id = sc.next();
                    System.out.print("请输入姓名：");
                    String name = sc.next();
                    System.out.print("请输入Java成绩：");
                    double javaScore = sc.nextDouble();
                    System.out.print("请输入MySQL成绩：");
                    double sqlScore = sc.nextDouble();
                    System.out.print("请输入英语成绩：");
                    double englishScore = sc.nextDouble();

                    boolean addResult = service.add(new Student(id, name, javaScore, sqlScore, englishScore));
                    System.out.println(addResult ? "添加成功" : "添加失败，学号已存在");
                    break;

                case "2":
                    service.showAll();
                    break;

                case "3":
                    System.out.print("请输入要查询的学号：");
                    String findId = sc.next();
                    Student stu = service.findById(findId);
                    System.out.println(stu == null ? "未找到该学生" : stu);
                    break;

                case "4":
                    System.out.print("请输入要修改的学号：");
                    String updateId = sc.next();
                    System.out.print("请输入新的Java成绩：");
                    double newJava = sc.nextDouble();
                    System.out.print("请输入新的MySQL成绩：");
                    double newSql = sc.nextDouble();
                    System.out.print("请输入新的英语成绩：");
                    double newEnglish = sc.nextDouble();
                    System.out.println(service.update(updateId, newJava, newSql, newEnglish) ? "修改成功" : "修改失败，学号不存在");
                    break;

                case "5":
                    System.out.print("请输入要删除的学号：");
                    String deleteId = sc.next();
                    System.out.println(service.deleteById(deleteId) ? "删除成功" : "删除失败，学号不存在");
                    break;

                case "6":
                    service.sortByTotalDesc();
                    service.showAll();
                    break;

                case "7":
                    service.statistics();
                    break;

                case "0":
                    System.out.println("系统已退出");
                    return;

                default:
                    System.out.println("输入有误，请重新选择");
            }
        }
    }
}