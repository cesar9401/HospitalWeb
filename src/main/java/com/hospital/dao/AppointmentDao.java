package com.hospital.dao;

import com.hospital.model.Appointment;
import com.hospital.model.Result;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author cesar31
 */
public class AppointmentDao {

    private Connection transaction;
    private final long CERO_H = 21600000;
    private final long UNO_H = 3600000;

    public AppointmentDao() {
    }

    public AppointmentDao(Connection transaction) {
        this.transaction = transaction;
    }

    /**
     * Metodo para insertar citas con el medico, recibo un objeto de tipo cita
     *
     * @param a
     */
    public void insertAppointment(Appointment a) {
        String query = "INSERT INTO APPOINTMENTS(appointment_id, patient_id, doctor_id, specialty_id, date, time, status) VALUES(?, ?, ?, "
                + "(SELECT specialty_id FROM SPECIALTIES WHERE degree = ? LIMIT 1)"
                + ", ?, ?, ?)";
        try (PreparedStatement pst = this.transaction.prepareStatement(query)) {
            pst.setInt(1, a.getAppointmentId());
            pst.setInt(2, a.getPatientId());
            pst.setString(3, a.getDoctorId());
            pst.setString(4, a.getDegree());
            pst.setDate(5, a.getDate());
            pst.setTime(6, a.getTime());
            pst.setBoolean(7, a.isStatus());
            pst.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace(System.out);
        }
    }

    /**
     * Metodo para insertar una nueva cita en el sistema
     *
     * @param a
     */
    public void insertNewAppointment(Appointment a) {
        String query = "INSERT INTO APPOINTMENTS(patient_id, doctor_id, specialty_id, date, time) VALUES(?, ?, ?, ?, ?)";
        try (PreparedStatement pst = this.transaction.prepareStatement(query)) {
            pst.setInt(1, a.getPatientId());
            pst.setString(2, a.getDoctorId());
            pst.setInt(3, a.getSpecialtyId());
            pst.setDate(4, a.getDate());
            pst.setTime(5, a.getTime());
            pst.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace(System.out);
        }
    }

    /**
     * Metodo para insertar una cita en laboratorio, recibe objeto de tipo cita
     *
     * @param r
     * @return
     */
    public int insertAppointmentLab(Result r) {
        int id = 0;
        String query = "INSERT INTO APPOINTMENTS_LAB(patient_id, doctor_id, exam_id, date, time, exam_order, status) VALUES(?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = this.transaction.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {
            //pst.setInt(1, a.getAppointmentId());
            pst.setInt(1, r.getPatientId());
            pst.setString(2, r.getDoctorId());
            pst.setInt(3, r.getExamId());
            pst.setDate(4, r.getDate());
            pst.setTime(5, r.getTime());
            pst.setBlob(6, r.getOrderResult());
            pst.setBoolean(7, r.isStatus());
            pst.executeUpdate();

            ResultSet rs = pst.getGeneratedKeys();
            if (rs.next()) {
                id = rs.getInt(1);
            }
        } catch (SQLException ex) {
            ex.printStackTrace(System.out);
        }
        return id;
    }

    /**
     * Metodo para obtener las citas medicas y de lab de algun paciente
     *
     * @param patientId
     * @param status
     * @param lab
     * @return
     */
    public List<Appointment> getAppointmentsByPatient(int patientId, boolean status, boolean lab) {
        List<Appointment> appointments = new ArrayList<>();
        String query;
        if (lab) {
            //query = "SELECT * FROM APPOINTMENTS_LAB WHERE patient_id = ? AND status = ? ORDER BY date, time";
            query = "SELECT a.*, d.name AS doctor_name, p.name AS patient_name, e.name AS exam_name FROM APPOINTMENTS_LAB a INNER JOIN DOCTORS d ON a.doctor_id = d.doctor_id INNER JOIN PATIENTS p ON a.patient_id = p.patient_id INNER JOIN EXAMS e ON a.exam_id = e.exam_id "
                    + "WHERE a.patient_id = ? AND status = ? ORDER BY date, time";
        } else {
            query = "SELECT a.*, d.name AS doctor_name, s.degree, p.name AS patient_name FROM APPOINTMENTS a INNER JOIN DOCTORS d ON a.doctor_id = d.doctor_id "
                    + "INNER JOIN SPECIALTIES s ON a.specialty_id = s.specialty_id INNER JOIN PATIENTS p ON a.patient_id = p.patient_id "
                    + "WHERE a.patient_id = ? AND a.status = ? ORDER BY date, time";
        }
        try (PreparedStatement pst = this.transaction.prepareStatement(query);) {
            pst.setInt(1, patientId);
            pst.setBoolean(2, status);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    appointments.add(new Appointment(rs, lab));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace(System.out);
        }

        return appointments;
    }

    /**
     * Metodo para obtener las citas de un medico segun determinada fecha
     *
     * @param doctorId
     * @param date
     * @param lab
     * @return
     */
    public List<Appointment> getAppointmentsByDoctor(String doctorId, java.sql.Date date, boolean lab) {
        List<Appointment> appointments = new ArrayList<>();
        String query = "SELECT a.*, d.name AS doctor_name, s.degree, p.name AS patient_name FROM APPOINTMENTS a INNER JOIN DOCTORS d ON a.doctor_id = d.doctor_id "
                + "INNER JOIN SPECIALTIES s ON a.specialty_id = s.specialty_id INNER JOIN PATIENTS p ON a.patient_id = p.patient_id "
                + "WHERE a.doctor_id = ? AND a.date = ? ORDER BY time";
        try (PreparedStatement pst = this.transaction.prepareStatement(query)) {
            pst.setString(1, doctorId);
            pst.setDate(2, date);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                appointments.add(new Appointment(rs, lab));
            }
        } catch (SQLException ex) {
            ex.printStackTrace(System.out);
        }
        return appointments;
    }

    public Appointment getAppointmentById(int appId, boolean lab) {
        Appointment app = null;
        String query;
        if (lab) {
            query = "SELECT a.*, d.name AS doctor_name, p.name AS patient_name, e.name AS exam_name FROM APPOINTMENTS_LAB a INNER JOIN DOCTORS d ON a.doctor_id = d.doctor_id INNER JOIN PATIENTS p ON a.patient_id = p.patient_id INNER JOIN EXAMS e ON a.exam_id = e.exam_id "
                    + "WHERE a.appointment_lab_id = ? LIMIT 1";
        } else {
            query = "SELECT a.*, d.name AS doctor_name, s.degree, p.name AS patient_name FROM APPOINTMENTS a INNER JOIN DOCTORS d ON a.doctor_id = d.doctor_id "
                    + "INNER JOIN SPECIALTIES s ON a.specialty_id = s.specialty_id INNER JOIN PATIENTS p ON a.patient_id = p.patient_id "
                    + "WHERE a.appointment_id = ? LIMIT 1";
        }

        try (PreparedStatement pst = this.transaction.prepareStatement(query)) {
            pst.setInt(1, appId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    app = new Appointment(rs, lab);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace(System.out);
        }
        return app;
    }

    public List<Appointment> getAppointmentLab(int examId, java.sql.Date date) {
        List<Appointment> app = new ArrayList<>();
        String query = "SELECT a.*, d.name AS doctor_name, p.name AS patient_name, e.name AS exam_name FROM APPOINTMENTS_LAB a INNER JOIN DOCTORS d ON a.doctor_id = d.doctor_id INNER JOIN PATIENTS p ON a.patient_id = p.patient_id INNER JOIN EXAMS e ON a.exam_id = e.exam_id "
                + "WHERE a.exam_id = ? AND a.date = ? ORDER BY date, time";
        try (PreparedStatement pst = this.transaction.prepareStatement(query)) {
            pst.setInt(1, examId);
            pst.setDate(2, date);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    app.add(new Appointment(rs, true));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace(System.out);
        }

        return app;
    }

    public void createAppointmentLab(Appointment a) {
        int id = 0;
        String query = "INSERT INTO APPOINTMENTS_LAB(patient_id, doctor_id, exam_id, date, time, exam_order, status) VALUES(?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = this.transaction.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {
            //pst.setInt(1, a.getAppointmentId());
            pst.setInt(1, a.getPatientId());
            pst.setString(2, a.getDoctorId());
            pst.setInt(3, a.getExamId());
            pst.setDate(4, a.getDate());
            pst.setTime(5, a.getTime());
            pst.setBlob(6, a.getOrder());
            pst.setBoolean(7, a.isStatus());
            pst.executeUpdate();

        } catch (SQLException ex) {
            ex.printStackTrace(System.out);
        }
    }

    public List<Appointment> getAppointmentTotalLab(int examId, java.sql.Date date) {
        List<Appointment> appLab = new ArrayList<>();
        List<Appointment> app = getAppointmentLab(examId, date);

        java.sql.Time time = new java.sql.Time(CERO_H);
        for (int i = 0; i < 24; i++) {
            boolean isApp = false;
            for (Appointment a : app) {
                if (time.equals(a.getTime())) {
                    appLab.add(a);
                    isApp = true;
                    break;
                }
            }
            if (!isApp) {
                java.sql.Time tmp = new Time(time.getTime());
                appLab.add(new Appointment(tmp, true));
            }
            time.setTime(time.getTime() + UNO_H);
        }

        return appLab;
    }

    public void getOrder(int appointmentLabId, HttpServletResponse response) {
        String query = "SELECT exam_order FROM APPOINTMENTS_LAB WHERE appointment_lab_id = ?";

        response.setContentType("application/pdf");
        InputStream inputStream = null;

        try (PreparedStatement pst = this.transaction.prepareStatement(query)) {
            pst.setInt(1, appointmentLabId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    inputStream = new ByteArrayInputStream(rs.getBytes("exam_order"));
                }
            }
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data, 0, inputStream.available());
            response.getOutputStream().write(data);
            inputStream.close();
        } catch (SQLException | IOException ex) {
            ex.printStackTrace(System.out);
        }
    }

}
