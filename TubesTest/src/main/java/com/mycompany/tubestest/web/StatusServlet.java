package com.mycompany.tubestest.web;

import com.mycompany.tubestest.LaundryService;
import com.mycompany.tubestest.OrderReport;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Servlet untuk meneruskan permintaan ke halaman JSP cek status.
 */
public class StatusServlet extends HttpServlet {

    private final LaundryService service = LaundryService.getInstance();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String orderId = req.getParameter("orderId");
        if (orderId != null && !orderId.isBlank()) {
            OrderReport report = service.findReport(orderId);
            req.setAttribute("orderId", orderId.trim());
            req.setAttribute("found", report != null);
            if (report != null) {
                req.setAttribute("status", report.getStatus());
                req.setAttribute("total", service.formatRupiah(report.getOrder().getTotalPrice()));
                req.setAttribute("complaint", report.getComplaint());
            }
        }
        req.getRequestDispatcher("/status.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        doGet(req, resp);
    }
}
