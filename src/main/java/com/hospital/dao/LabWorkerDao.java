package com.hospital.dao;

import com.hospital.model.Day;
import com.hospital.model.LabWorker;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author cesar31
 */
public class LabWorkerDao {

    private Connection transaction;

    public LabWorkerDao() {
    }

    public LabWorkerDao(Connection transaction) {
        this.transaction = transaction;
    }

    /**
     * Metodo para insertar a un trabajador de laboratorio a la bd
     *
     * @param lab
     */
    public void insertLabWoker(LabWorker lab) {
        String query = "INSERT INTO LAB_WORKERS(lab_worker_id, name, registry_number, dpi, phone, email, start_date, password, exam_id) VALUES(?, ?, ?, ?, ?, ?, ?, ?, "
                + "(SELECT exam_id FROM EXAMS WHERE name = ? LIMIT 1))";

        try (PreparedStatement pst = this.transaction.prepareStatement(query)) {
            pst.setString(1, lab.getLabWorkerId());
            pst.setString(2, lab.getName());
            pst.setString(3, lab.getRegistry());
            pst.setString(4, lab.getDpi());
            pst.setString(5, lab.getPhone());
            pst.setString(6, lab.getEmail());
            pst.setDate(7, lab.getStartDate());
            pst.setString(8, lab.getPass());
            pst.setString(9, lab.getExamName());
            pst.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace(System.out);
        }
    }

    /**
     * Metodo para insertar los dias que trabajo un laboratorista
     *
     * @param lab
     */
    public void insertLabWorkersDays(LabWorker lab) {
        String query = "INSERT INTO WORKER_DAYS(lab_worker_id, day_id) VALUES(?, ?)";
        try (PreparedStatement pst = this.transaction.prepareStatement(query)) {
            for (Day d : lab.getDays()) {
                pst.setString(1, lab.getLabWorkerId());
                pst.setInt(2, d.getDayId());
                pst.executeUpdate();
            }
        } catch (SQLException ex) {
            ex.printStackTrace(System.out);
        }
    }

    /**
     * Metodo para obtener un laboratorista segun su email y pass
     *
     * @param email
     * @param pass
     * @return
     */
    public LabWorker getLabWorker(String email, String pass) {
        LabWorker lab = null;
        //String query = "SELECT * FROM LAB_WORKERS WHERE email = ? and password = ?";
        String query = "SELECT l.*, GROUP_CONCAT(d.name_day) AS name_days FROM LAB_WORKERS l INNER JOIN WORKER_DAYS w ON l.lab_worker_id = w.lab_worker_id INNER JOIN DAYS d ON w.day_id = d.day_id "
                + "WHERE l.email = ? and l.password = ? GROUP BY l.lab_worker_id";
        try (PreparedStatement pst = this.transaction.prepareStatement(query)) {
            pst.setString(1, email);
            pst.setString(2, pass);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    lab = new LabWorker(rs);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace(System.out);
        }
        return lab;
    }

    /**
     * Obtener un laboratorista segun su id
     *
     * @param labId
     * @return
     */
    public LabWorker getLabWorkerById(String labId) {
        LabWorker lab = null;
        String query = "SELECT l.*, GROUP_CONCAT(d.name_day) AS name_days FROM LAB_WORKERS l INNER JOIN WORKER_DAYS w ON l.lab_worker_id = w.lab_worker_id INNER JOIN DAYS d ON w.day_id = d.day_id "
                + "WHERE l.lab_worker_id = ? GROUP BY l.lab_worker_id";
        try (PreparedStatement pst = this.transaction.prepareStatement(query)) {
            pst.setString(1, labId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    lab = new LabWorker(rs);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace(System.out);
        }
        return lab;
    }

    /**
     * Metodo para obtener listado de trabajadores de laboratorio
     *
     * @return
     */
    public List<LabWorker> getLabWorkers() {
        List<LabWorker> labs = new ArrayList<>();
        //String query = "SELECT * FROM LAB_WORKERS";
        String query = "SELECT l.*, GROUP_CONCAT(d.name_day) AS name_days FROM LAB_WORKERS l INNER JOIN WORKER_DAYS w ON l.lab_worker_id = w.lab_worker_id INNER JOIN DAYS d ON w.day_id = d.day_id "
                + "GROUP BY l.lab_worker_id";
        try (PreparedStatement pst = this.transaction.prepareStatement(query); ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                labs.add(new LabWorker(rs));
            }
        } catch (SQLException ex) {
            ex.printStackTrace(System.out);
        }
        return labs;
    }
}
