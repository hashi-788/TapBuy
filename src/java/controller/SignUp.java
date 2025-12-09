package controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dto.Response_DTO;
import dto.User_DTO;
import entity.User;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.HibernateUtil;
import model.Mail;
import model.Validations;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

@WebServlet(name = "SignUp", urlPatterns = {"/SignUp"})
public class SignUp extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Response_DTO response_DTO = new Response_DTO();
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

        User_DTO user_DTO = gson.fromJson(request.getReader(), User_DTO.class);

        if (user_DTO.getFirst_name().isEmpty()) {
            response_DTO.setContent("Please Enter Your First Name");
        } else if (user_DTO.getLast_name().isEmpty()) {
            response_DTO.setContent("Please Enter Your Last Name");
        } else if (user_DTO.getEmail().isEmpty()) {
            response_DTO.setContent("Please Enter Your Email");
        } else if (!Validations.isEmailValid(user_DTO.getEmail())) {
            response_DTO.setContent("Please Enter Valid Email");
        } else if (user_DTO.getPassword().isEmpty()) {
            response_DTO.setContent("Please Enter Your Password");
        } else if (!Validations.isPasswordValid(user_DTO.getPassword())) {
            response_DTO.setContent("Passowrd must include at least one uppercase letter,number,specila character and be at least eight characters.");
        } else {

            Session session = HibernateUtil.getSessionFactory().openSession();
           
            Criteria criteria1 = session.createCriteria(User.class);
            criteria1.add(Restrictions.eq("email", user_DTO.getEmail()));

            if (!criteria1.list().isEmpty()) {
                response_DTO.setContent("User with this Email already exists");
            } else {
                int code = (int) (Math.random() * 1000000);

                final User user = new User();
                user.setEmail(user_DTO.getEmail());
                user.setFirst_name(user_DTO.getFirst_name());
                user.setLast_name(user_DTO.getLast_name());
                user.setPassword(user_DTO.getPassword());
                user.setVerification(String.valueOf(code));
                user.setRest_verification("");

                Thread sendMailThread = new Thread() {
                    @Override
                    public void run() {
                        Mail.sendMail(user.getEmail(), "Tap Buy Verification",
                                "<h1 style=\"color:#6482AD;\">Your Verifification Code: " + user.getVerification() + "</h1>"
                        );
                    }
                };
                sendMailThread.start();

                session.save(user);
                session.beginTransaction().commit();

                request.getSession().setAttribute("email", user_DTO.getEmail());
                response_DTO.setSuccess(true);
                response_DTO.setContent("Registration Complete. Please check your inbox for verification code.");
            }
            session.close();
        }
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(response_DTO));
    }
}
