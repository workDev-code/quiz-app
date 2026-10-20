package com.anhtduong.quiz.seed;

import com.anhtduong.quiz.question.AnswerOptionEntity;
import com.anhtduong.quiz.question.QuestionEntity;
import com.anhtduong.quiz.question.QuizQuestionEntity;
import com.anhtduong.quiz.quiz.QuizEntity;
import com.anhtduong.quiz.user.UserEntity;
import com.anhtduong.quiz.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Chỉ chạy với profile {@code dev} hoặc {@code test}. Tạo người dùng + bài kiểm tra mẫu
 * đúng cấu trúc với file {@code demo/Ánh Dương Quiz.html}. Mật khẩu mặc định: {@code Demo@1234}.
 */
@Component
@Profile({"dev", "test"})
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEFAULT_PASSWORD = "Demo@1234";

    private final UserRepository users;
    private final com.anhtduong.quiz.quiz.QuizRepository quizzes;
    private final com.anhtduong.quiz.question.QuestionRepository questions;
    private final com.anhtduong.quiz.question.QuizQuestionRepository quizQuestions;
    private final PasswordEncoder encoder;
    private final boolean enabled;

    public DataSeeder(UserRepository users,
                      com.anhtduong.quiz.quiz.QuizRepository quizzes,
                      com.anhtduong.quiz.question.QuestionRepository questions,
                      com.anhtduong.quiz.question.QuizQuestionRepository quizQuestions,
                      PasswordEncoder encoder,
                      @Value("${app.seed.enabled:true}") boolean enabled) {
        this.users = users;
        this.quizzes = quizzes;
        this.questions = questions;
        this.quizQuestions = quizQuestions;
        this.encoder = encoder;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) return;
        if (users.count() > 0) {
            log.info("DataSeeder: đã có dữ liệu, bỏ qua seed.");
            return;
        }
        log.info("DataSeeder: tạo dữ liệu mẫu (chỉ chạy ở profile dev/test) - mật khẩu mặc định: {}", DEFAULT_PASSWORD);

        createUser("Nguyễn Thu Lan", "hocsinh@demo.vn", UserEntity.Role.STUDENT);
        createUser("Trần Quốc Bảo",  "giaovien@demo.vn", UserEntity.Role.TEACHER);
        createUser("Lê Hoàng Minh",  "minh@demo.vn", UserEntity.Role.STUDENT);
        createUser("Phạm Ngọc Hà",   "ha@demo.vn",   UserEntity.Role.STUDENT);

        Instant now = Instant.now();
        seedQuiz("Java Core: biến và kiểu dữ liệu",
                "Ôn lại các kiểu dữ liệu nguyên thuỷ, phép chia số nguyên, hằng số và so sánh chuỗi trong Java.",
                "Lập trình", QuizEntity.Status.PUBLISHED, 10, 3, 60,
                now.plus(20, ChronoUnit.DAYS), now.minus(30, ChronoUnit.DAYS), now.minus(9, ChronoUnit.DAYS),
                List.of(
                        qe("Kiểu dữ liệu nào lưu số nguyên 32 bit trong Java?",
                                "int có kích thước 32 bit, phạm vi khoảng ±2,1 tỷ.",
                                List.of("byte","int","long","double"), 1, "EASY"),
                        qe("Kết quả của 7 / 2 khi cả hai toán hạng là int?",
                                "Phép chia hai số nguyên trong Java bỏ phần thập phân nên 7 / 2 = 3.",
                                List.of("3.5","4","3","Lỗi biên dịch"), 2, "EASY"),
                        qe("Từ khoá nào khai báo hằng số, không cho gán lại giá trị?",
                                "final ngăn việc gán lại biến sau khi đã khởi tạo. const là từ khoá dành riêng nhưng Java không dùng.",
                                List.of("static","final","const","volatile"), 1, "MEDIUM"),
                        qe("Giá trị mặc định của một thuộc tính boolean trong lớp là gì?",
                                "Thuộc tính boolean chưa gán giá trị mang giá trị mặc định false.",
                                List.of("true","false","null","0"), 1, "EASY"),
                        qe("So sánh nội dung hai chuỗi String nên dùng gì?",
                                "== so sánh tham chiếu, còn equals() so sánh nội dung chuỗi.",
                                List.of("==","equals()","compare()","is()"), 1, "MEDIUM"),
                        qe("Cách khai báo mảng số nguyên nào hợp lệ?",
                                "Cú pháp chuẩn là int[] arr = new int[3];",
                                List.of("int arr = new int[3];","int[] arr = new int[3];","array<int> arr = new array(3);","int arr[] = (3);"), 1, "HARD")
                ));

        seedQuiz("SQL cơ bản: SELECT và JOIN",
                "Bài kiểm tra ngắn về truy vấn dữ liệu, nối bảng và khoá chính.",
                "Cơ sở dữ liệu", QuizEntity.Status.PUBLISHED, 8, 2, 50,
                now.minus(3, ChronoUnit.DAYS), now.minus(40, ChronoUnit.DAYS), now.minus(12, ChronoUnit.DAYS),
                List.of(
                        qe("Câu lệnh nào lấy tất cả cột của bảng students?", "SELECT * FROM tên_bảng lấy mọi cột.",
                                List.of("GET * FROM students","SELECT * FROM students","SHOW students","FETCH students.*"), 1, "EASY"),
                        qe("INNER JOIN trả về những dòng nào?",
                                "INNER JOIN chỉ giữ các dòng thoả điều kiện nối ở cả hai bảng.",
                                List.of("Mọi dòng của bảng bên trái","Các dòng khớp điều kiện ở cả hai bảng","Mọi dòng của cả hai bảng","Chỉ các dòng không khớp"), 1, "MEDIUM"),
                        qe("Mệnh đề nào lọc kết quả sau khi nhóm bằng GROUP BY?",
                                "WHERE lọc dòng trước khi nhóm, HAVING lọc nhóm sau khi tính hàm tổng hợp.",
                                List.of("WHERE","HAVING","FILTER","LIMIT"), 1, "MEDIUM"),
                        qe("Khoá chính (PRIMARY KEY) đảm bảo điều gì?", "Khoá chính vừa duy nhất vừa không được NULL.",
                                List.of("Giá trị duy nhất và không NULL","Chỉ không NULL","Chỉ duy nhất","Luôn là số"), 0, "EASY")
                ));

        seedQuiz("Tiếng Anh: Present Perfect",
                "Chọn đáp án đúng cho các câu dùng thì hiện tại hoàn thành.",
                "Tiếng Anh", QuizEntity.Status.PUBLISHED, 6, 3, 60,
                null, now.minus(25, ChronoUnit.DAYS), now.minus(7, ChronoUnit.DAYS),
                List.of(
                        qe("She ___ in Hanoi for five years.", "\"for five years\" chỉ khoảng thời gian kéo dài đến hiện tại nên dùng hiện tại hoàn thành.",
                                List.of("lives","has lived","is living","lived"), 1, "MEDIUM"),
                        qe("I ___ my homework yet.", "\"yet\" trong câu phủ định đi với have/has not + V3.",
                                List.of("don't finish","haven't finished","didn't finished","am not finish"), 1, "MEDIUM"),
                        qe("They ___ to Da Nang three times.", "Chủ ngữ số nhiều \"They\" đi với have been.",
                                List.of("have been","has been","were","are going"), 0, "EASY"),
                        qe("Chọn câu đúng.", "Dạng V3 của go là gone.",
                                List.of("He has went home.","He has gone home.","He has go home.","He have gone home."), 1, "HARD"),
                        qe("___ you ever tried pho?", "Câu hỏi trải nghiệm dùng Have you ever + V3.",
                                List.of("Did","Do","Have","Has"), 2, "MEDIUM")
                ));

        seedQuiz("HTTP và REST cơ bản",
                "Mã trạng thái, phương thức HTTP và cách đặt tên endpoint.",
                "Lập trình", QuizEntity.Status.DRAFT, 12, 2, 60,
                null, now.minus(3, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS),
                List.of(
                        qe("Mã trạng thái nào cho biết tạo mới tài nguyên thành công?", "201 Created dùng sau khi POST tạo tài nguyên.",
                                List.of("200","201","204","301"), 1, "EASY"),
                        qe("Phương thức HTTP nào thường dùng để thay thế toàn bộ một tài nguyên?", "PUT thay thế toàn bộ tài nguyên, PATCH sửa một phần.",
                                List.of("GET","PUT","HEAD","TRACE"), 1, "MEDIUM"),
                        qe("Mã 401 khác 403 ở điểm nào?", "",
                                List.of("Hai mã giống nhau","401: chưa xác thực, 403: không đủ quyền","401: lỗi máy chủ","403: không tìm thấy"), 1, "MEDIUM")
                ));

        log.info("DataSeeder: xong. Tổng: {} người dùng, {} bài kiểm tra.", users.count(), quizzes.count());
    }

    private void createUser(String name, String email, UserEntity.Role role) {
        if (users.existsByEmail(email)) return;
        UserEntity u = new UserEntity();
        u.setName(name);
        u.setEmail(email);
        u.setRole(role);
        u.setPasswordHash(encoder.encode(DEFAULT_PASSWORD));
        users.save(u);
    }

    /**
     * Tạo câu hỏi trong ngân hàng và liên kết vào quiz qua bảng quiz_questions.
     */
    private QuestionEntity qe(String text, String expl, List<String> options, int correctIdx, String difficulty) {
        QuestionEntity qe = new QuestionEntity();
        qe.setText(text);
        qe.setExplanation(expl);
        qe.setDifficulty(difficulty);
        qe.setType("SINGLE_CHOICE");
        int i = 0;
        for (String t : options) {
            AnswerOptionEntity o = new AnswerOptionEntity();
            o.setQuestion(qe);
            o.setText(t);
            o.setCorrect(i == correctIdx);
            o.setPosition(i);
            qe.getOptions().add(o);
            i++;
        }
        return qe;
    }

    private void seedQuiz(String title, String desc, String subject, QuizEntity.Status status,
                          int dur, int max, int pass, Instant closesAt, Instant createdAt, Instant updatedAt,
                          List<QuestionEntity> questionSpecs) {
        QuizEntity q = new QuizEntity();
        q.setTitle(title);
        q.setDescription(desc);
        q.setSubject(subject);
        q.setStatus(status);
        q.setDurationMin(dur);
        q.setMaxAttempts(max);
        q.setPassPct(pass);
        q.setClosesAt(closesAt);
        q.setCreatedAt(createdAt);
        q.setUpdatedAt(updatedAt);
        q = quizzes.save(q);

        int pos = 0;
        for (QuestionEntity spec : questionSpecs) {
            // Bỏ qua nếu đã tồn tại (idempotent)
            QuestionEntity saved = questions.save(spec);
            QuizQuestionEntity qq = new QuizQuestionEntity();
            qq.setQuizId(q.getId());
            qq.setQuestionId(saved.getId());
            qq.setPosition(pos++);
            quizQuestions.save(qq);
        }
    }
}
