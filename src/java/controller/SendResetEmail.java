package controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dto.Response_DTO;
import dto.User_DTO;
import entity.User;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import model.HibernateUtil;
import model.Mail;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

@WebServlet(name = "SendResetEmail", urlPatterns = {"/SendResetEmail"})
public class SendResetEmail extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        Response_DTO response_DTO = new Response_DTO();
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

        try {
            User_DTO user_DTO = gson.fromJson(request.getReader(), User_DTO.class);

            Criteria criteria = session.createCriteria(User.class);
            criteria.add(Restrictions.eq("email", user_DTO.getEmail()));

            User user = (User) criteria.uniqueResult();

            if (user == null) {
                response_DTO.setSuccess(false);
                response_DTO.setContent("Email not found. Please check again.");
            } else {
               
                int restCode = (int) (Math.random() * 900000) + 100000;
                user.setRest_verification(String.valueOf(restCode));

                tx = session.beginTransaction();
                session.update(user);
                tx.commit();

                // ✅ Store email in HTTP session for later use (ForgetPassword)
                HttpSession httpSession = request.getSession(true);
                httpSession.setAttribute("email", user.getEmail());

                final String userEmail = user.getEmail();
                final String code = user.getRest_verification();

                Thread mailThread = new Thread(() -> {
                    Mail.sendMail(userEmail, "Tap Buy Password Reset",
                            "<h2>Your verification code: " + code + "</h2>");
                });
                mailThread.start(); 

                response_DTO.setSuccess(true);
                response_DTO.setContent("Verification code sent to your email.");
            }

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            response_DTO.setSuccess(false);
            response_DTO.setContent("Internal server error.");
        } finally {
            session.close();
        }

        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(response_DTO));
    }
}
