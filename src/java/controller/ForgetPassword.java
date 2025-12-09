package controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dto.Response_DTO;
import entity.User;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import model.HibernateUtil;
import model.Validations;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

@WebServlet(name = "ForgetPassword", urlPatterns = {"/ForgetPassword"})
public class ForgetPassword extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Response_DTO response_DTO = new Response_DTO();
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

        try {
            JsonObject dto = gson.fromJson(request.getReader(), JsonObject.class);

            String newPassword = dto.get("password").getAsString();
            String confirmPassword = dto.get("confirmPassword").getAsString();
            String resetCode = dto.get("verificationCode").getAsString();

            HttpSession httpSession = request.getSession(false);
            Object emailObj = (httpSession != null) ? httpSession.getAttribute("email") : null;

            if (emailObj == null) {
                response_DTO.setContent("Session expired. Please try again.");
            } else {
                String email = emailObj.toString();

                if (newPassword.isEmpty()) {
                    response_DTO.setContent("Please fill new Password.");
                } else if (!Validations.isPasswordValid(newPassword)) {
                    response_DTO.setContent("Password must include at least one uppercase letter, number, special character, and be at least eight characters long.");
                } else if (confirmPassword.isEmpty()) {
                    response_DTO.setContent("Please fill confirm Password.");
                } else if (!newPassword.equals(confirmPassword)) {
                    response_DTO.setContent("Passwords do not match.");
                } else {
                    Session session = HibernateUtil.getSessionFactory().openSession();
                    Transaction tx = session.beginTransaction();

                    Criteria criteria = session.createCriteria(User.class);
                    criteria.add(Restrictions.eq("email", email));
                    User user = (User) criteria.uniqueResult();

                    if (user == null) {
                        response_DTO.setContent("User not found.");
                    } else if (!user.getRest_verification().equals(resetCode)) {
                        response_DTO.setContent("Invalid verification code.");
                    } else {
                        user.setPassword(newPassword);
                        user.setRest_verification("");
                        session.update(user);
                        tx.commit();

                        response_DTO.setSuccess(true);
                        response_DTO.setContent("Password reset successful");
                    }

                    session.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response_DTO.setContent("Server error while resetting password.");
        }

        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(response_DTO));
    }
}
