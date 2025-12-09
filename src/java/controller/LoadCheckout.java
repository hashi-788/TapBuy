package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dto.User_DTO;
import entity.Address;
import entity.Cart;
import entity.City;
import entity.User;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import model.HibernateUtil;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

@WebServlet(name = "LoadCheckout", urlPatterns = {"/LoadCheckout"})
public class LoadCheckout extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Gson gson = new Gson();
        JsonObject json = new JsonObject();
        json.addProperty("success", false);

        HttpSession httpSession = request.getSession();
        Session session = HibernateUtil.getSessionFactory().openSession();

        try {
            if (httpSession.getAttribute("user") != null) {

                User_DTO userDTO = (User_DTO) httpSession.getAttribute("user");

                Criteria c1 = session.createCriteria(User.class);
                c1.add(Restrictions.eq("email", userDTO.getEmail()));
                User user = (User) c1.uniqueResult();

                Criteria c2 = session.createCriteria(Address.class);
                c2.add(Restrictions.eq("user", user));
                c2.addOrder(Order.desc("id"));
                c2.setMaxResults(1);
                Address address = (Address) c2.uniqueResult();

                Criteria c3 = session.createCriteria(City.class);
                c3.addOrder(Order.asc("name"));
                List<City> cityList = c3.list();

                Criteria c4 = session.createCriteria(Cart.class);
                c4.add(Restrictions.eq("user", user));
                List<Cart> cartList = c4.list();

                // address pack
                if (address != null) {
                    address.setUser(null);
                    json.add("address", gson.toJsonTree(address));
                } else {
                    json.add("address", null);
                }

                json.add("cityList", gson.toJsonTree(cityList));

                if (cartList == null) {
                    cartList = new ArrayList<>();
                }
                json.add("cartList", gson.toJsonTree(cartList));

                json.addProperty("success", true);

            } else {
                json.addProperty("message", "Not signed in");
            }

        } catch (Exception e) {
            e.printStackTrace();
            json.addProperty("message", "Server error");
        }

        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(json));

        session.close();
    }
}
