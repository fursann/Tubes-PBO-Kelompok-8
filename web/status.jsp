<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cek Status — CleanHub</title>
    <style>
        body { font-family: "Segoe UI", sans-serif; background: #0f172a; color: #f1f5f9; padding: 24px; max-width: 520px; margin: 0 auto; }
        .card { background: #1e293b; border-radius: 12px; padding: 20px; border: 1px solid #334155; }
        h1 { color: #38bdf8; font-size: 1.25rem; }
        label { display: block; margin: 12px 0 6px; color: #94a3b8; font-size: 0.85rem; }
        input { width: 100%; padding: 10px; border-radius: 8px; border: 1px solid #334155; background: #0f172a; color: #f1f5f9; }
        button { margin-top: 12px; padding: 10px 16px; background: #38bdf8; color: #0f172a; border: none; border-radius: 8px; font-weight: 600; cursor: pointer; }
        .result { margin-top: 16px; padding: 12px; background: #0f172a; border-radius: 8px; }
        a { color: #38bdf8; }
        .err { color: #f87171; }
        .ok { color: #34d399; }
    </style>
</head>
<body>
<div class="card">
    <h1>Cek Status Pesanan (JSP)</h1>
    <p style="color:#94a3b8;font-size:0.9rem">Halaman ini diproses servlet + JSP, data dari <code>LaundryService</code> Java.</p>

    <form method="get" action="<%= request.getContextPath() %>/status">
        <label for="orderId">ID Pesanan</label>
        <input type="text" id="orderId" name="orderId" value="<%= request.getAttribute("orderId") != null ? request.getAttribute("orderId") : "" %>" placeholder="ORD-991">
        <button type="submit">Cek Status</button>
    </form>

    <%
        Boolean found = (Boolean) request.getAttribute("found");
        if (found != null) {
            if (found) {
    %>
    <div class="result ok">
        <strong><%= request.getAttribute("orderId") %></strong><br>
        Status: <%= request.getAttribute("status") %><br>
        Total: <%= request.getAttribute("total") %><br>
        <%
            String complaint = (String) request.getAttribute("complaint");
            if (complaint != null && !"-".equals(complaint)) {
        %>
        Keluhan: <%= complaint %>
        <% } %>
    </div>
    <%
            } else {
    %>
    <div class="result err">Pesanan tidak ditemukan.</div>
    <%
            }
        }
    %>

    <p style="margin-top:16px"><a href="index.html">← Kembali ke aplikasi</a></p>
</div>
</body>
</html>
