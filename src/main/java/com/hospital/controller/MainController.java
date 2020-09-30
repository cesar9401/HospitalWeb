package com.hospital.controller;

import com.hospital.conexion.Conexion;
import com.hospital.dao.*;
import com.hospital.model.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

/**
 *
 * @author cesar31
 */
@WebServlet(name = "MainController", urlPatterns = {"/MainController"})
@MultipartConfig(maxFileSize = 16177215)
public class MainController extends HttpServlet {

    private final Connection conexion = Conexion.getConnection();
    private final ResultDao resultDao = new ResultDao(conexion);
    private final ReportDao reportDao = new ReportDao(conexion);
    private final AppointmentDao appointmentDao = new AppointmentDao(conexion);
    private final PatientDao patientDao = new PatientDao(conexion);
    private final SpecialtyDao specialtyDao = new SpecialtyDao(conexion);
    private final DoctorDao doctorDao = new DoctorDao(conexion);
    private final ExamDao examDao = new ExamDao(conexion);
    private final AdministratorDao administratorDao = new AdministratorDao(conexion);
    private final LabWorkerDao labWorkerDao = new LabWorkerDao(conexion);
    
    public Connection getConexion() {
        return conexion;
    }
    
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet ControllerAdmin</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet ControllerAdmin at " + request.getContextPath() + "</h1>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        System.out.println("action = " + action);
        switch (action) {
            case "singOff":
                HttpSession session = request.getSession();
                session.invalidate();
                request.getRequestDispatcher("index.jsp").forward(request, response);
                //response.sendRedirect("index.jsp");
                break;
            case "newAppointment":
                List<Doctor> doctors = doctorDao.getDoctors();
                request.getSession().setAttribute("doctors", doctors);
                request.getRequestDispatcher("appointment.jsp").forward(request, response);
                break;
            case "myProfile":
                int patientId = (int) request.getSession().getAttribute("user");
                Patient p = patientDao.getPatientById(patientId);
                setProfilePatient(request, response, p);
                break;
            default:
                String doctorId = action;
                java.sql.Date date = (java.sql.Date) request.getSession().getAttribute("date");
                if (date == null) {
                    java.util.Date now = new java.util.Date();
                    date = new java.sql.Date(now.getTime());
                    request.getSession().setAttribute("date", date);
                }
                Doctor doctor = doctorDao.getDoctor(doctorId);
                doctor.setSpecialties(specialtyDao.getSpecialtiesByDoctor(doctorId));
                List<Appointment> app = getAppointmentsByDoctor(doctor, date, false);
                request.getSession().setAttribute("doctor", doctor);
                request.getSession().setAttribute("appointments", app);
                request.getSession().setAttribute("success", null);
                request.getRequestDispatcher("newAppointment.jsp").forward(request, response);
                break;
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        switch (action) {
            case "load":
                loadData(request, response);
                break;
            case "login":
                initLogin(request, response);
                break;
            case "d_specialties":
                Integer specialtyId = Integer.parseInt(request.getParameter("specialties"));
                java.sql.Date date = ReadXml.getDate(request.getParameter("date"));
                List<Doctor> doctors = doctorDao.getDoctorsBySpeciality(specialtyId);
                request.getSession().setAttribute("doctors", doctors);
                request.getSession().setAttribute("date", date);
                request.getRequestDispatcher("appointment.jsp").forward(request, response);
                break;
            case "changeDate":
                changeDateApp(request, response);
                break;
            case "newAppointment":
                setNewAppointment(request, response);
                break;
        }
    }

    private void loadData(HttpServletRequest request, HttpServletResponse response) {
        try {
            Part filePart = request.getPart("file");
            ReadXml read = new ReadXml(filePart);
            read.laodData();

        } catch (IOException | ServletException ex) {
            ex.printStackTrace(System.out);
        }
    }

    private void changeDateApp(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        java.sql.Date date = ReadXml.getDate(request.getParameter("date"));
        String doctorId = request.getParameter("doctorId");
        Doctor doctor = doctorDao.getDoctor(doctorId);
        doctor.setSpecialties(specialtyDao.getSpecialtiesByDoctor(doctorId));
        List<Appointment> app = getAppointmentsByDoctor(doctor, date, false);

        request.getSession().setAttribute("date", date);
        request.getSession().setAttribute("doctor", doctor);
        request.getSession().setAttribute("appointments", app);
        request.getRequestDispatcher("newAppointment.jsp").forward(request, response);
    }

    private void setNewAppointment(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String doctorId = request.getParameter("doctorId");
        int patientId = Integer.parseInt(request.getParameter("patientId"));
        int specialty = Integer.parseInt(request.getParameter("specialty"));
        java.sql.Time time = ReadXml.getTime(request.getParameter("AppTime"));
        java.sql.Date date = (java.sql.Date) request.getSession().getAttribute("date");
        Appointment tmp = new Appointment(patientId, doctorId, specialty, date, time);
        appointmentDao.insertNewAppointment(tmp);

        Doctor doctor = doctorDao.getDoctor(doctorId);
        doctor.setSpecialties(specialtyDao.getSpecialtiesByDoctor(doctorId));
        List<Appointment> app = getAppointmentsByDoctor(doctor, date, false);

        request.getSession().setAttribute("date", date);
        request.getSession().setAttribute("doctor", doctor);
        request.getSession().setAttribute("appointments", app);
        request.getSession().setAttribute("appTmp", tmp);
        request.getSession().setAttribute("success", true);
        request.getRequestDispatcher("newAppointment.jsp").forward(request, response);
    }

    private void initLogin(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = request.getParameter("email");
        String pass = request.getParameter("pass");
        String value = request.getParameter("tipoUsuario");
        switch (value) {
            case "PATIENTS":
                Patient p = patientDao.getPatien(email, pass);
                if (p != null) {
                    setProfilePatient(request, response, p);
                } else {
                    setErrorLogin(request, response);
                }
                break;
            case "DOCTORS":
                Doctor doctor = doctorDao.getDoctor(email, pass);
                if (doctor != null) {
                    setProfileDoctor(request, response, doctor);
                } else {
                    setErrorLogin(request, response);
                }
                break;
            case "LAB_WORKERS":
                LabWorker l = labWorkerDao.getLabWorker(email, pass);
                if (l != null) {
                    System.out.println(l.toString());
                } else {
                    setErrorLogin(request, response);

                }
                break;
            case "ADMINISTRATORS":
                Administrator a = administratorDao.getAdminById(email, pass);

                if (a != null) {
                    List<Specialty> specialties = specialtyDao.getSpecialties();
                    List<Exam> exams = examDao.getExams();

                    request.getSession().setAttribute("user", a.getAdminId());
                    request.getSession().setAttribute("profile", a);
                    request.getSession().setAttribute("specialties", specialties);
                    request.getSession().setAttribute("exams", exams);
                    request.getRequestDispatcher("adminView.jsp").forward(request, response);
                } else {
                    setErrorLogin(request, response);
                }
                break;
        }
    }

    /**
     * Metodo para dirigir al perfil del paciente
     *
     * @param request
     * @param response
     * @param p
     * @throws ServletException
     * @throws IOException
     */
    private void setProfilePatient(HttpServletRequest request, HttpServletResponse response, Patient p) throws ServletException, IOException {
        List<Result> results = resultDao.getResultsByPatient(p.getPatientId());
        List<Report> reports = reportDao.getReportsByPatient(p.getPatientId());
        List<Appointment> app = appointmentDao.getAppointmentsByPatient(p.getPatientId(), false, false);
        List<Appointment> appLab = appointmentDao.getAppointmentsByPatient(p.getPatientId(), false, true);
        List<Specialty> specialties = specialtyDao.getSpecialties();

        request.getSession().setAttribute("user", p.getPatientId());
        request.getSession().setAttribute("profile", p);
        request.getSession().setAttribute("results", results);
        request.getSession().setAttribute("reports", reports);
        request.getSession().setAttribute("app", app);
        request.getSession().setAttribute("appLab", appLab);
        request.getSession().setAttribute("specialties", specialties);
        request.getRequestDispatcher("patientView.jsp").forward(request, response);
    }

    public void setProfileDoctor(HttpServletRequest request, HttpServletResponse response, Doctor doctor) throws ServletException, IOException {
        doctor.setSpecialties(specialtyDao.getSpecialtiesByDoctor(doctor.getDoctorId()));
        request.getSession().setAttribute("user", doctor.getDoctorId());
        request.getSession().setAttribute("profile", doctor);
        java.sql.Date date = (java.sql.Date) request.getSession().getAttribute("date");
        if (date == null) {
            java.util.Date now = new java.util.Date();
            date = new java.sql.Date(now.getTime());
            request.getSession().setAttribute("date", date);
        } else {
            request.getSession().setAttribute("date", date);
        }
        List<Appointment> appointments = getAppointmentsByDoctor(doctor, date, false);
        request.getSession().setAttribute("appDoc", appointments);
        request.getRequestDispatcher("doctorView.jsp").forward(request, response);
    }

    /**
     * Envia mensaje de error si no se encuentra algun usuario con email y
     * passwords proporcionados
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    private void setErrorLogin(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getSession().setAttribute("error", true);
        request.getRequestDispatcher("index.jsp").forward(request, response);
    }

    /**
     * Metodo para obtener el listado de citas de un doctor por día
     *
     * @param doc
     * @param date
     * @param status
     * @param lab
     * @return
     */
    private List<Appointment> getAppointmentsByDoctor(Doctor doc, java.sql.Date date, boolean lab) {
        List<Appointment> app = appointmentDao.getAppointmentsByDoctor(doc.getDoctorId(), date, lab);
        List<Appointment> newApp = new ArrayList<>();
        long dif = doc.getEndTime().getTime() - doc.getStartTime().getTime();
        int hours = (int) TimeUnit.MILLISECONDS.toHours(dif);
        long milli = dif / hours;
        java.sql.Time time = new java.sql.Time(doc.getStartTime().getTime());

        for (int i = 0; i < hours; i++) {
            boolean isApp = false;
            for (Appointment a : app) {
                if (time.equals(a.getTime())) {
                    newApp.add(a);
                    isApp = true;
                    break;
                }
            }
            if (!isApp) {
                java.sql.Time tmp = new Time(time.getTime());
                newApp.add(new Appointment(tmp, true));
            }
            time.setTime(time.getTime() + milli);
        }
        return newApp;
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
