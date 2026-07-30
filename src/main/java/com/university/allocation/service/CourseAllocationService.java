package com.university.allocation.service;

import com.university.allocation.exception.AllocationException;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * Spring-managed service wrapping the original "University Course Allocation"
 * algorithm (ported earlier from C++ to Java).
 *
 * ONLY the I/O boundary changed vs. the original CourseAllocation.java:
 *   - static fields          -> instance fields (reset on every call, so the
 *                               bean is safe to reuse across concurrent HTTP
 *                               requests)
 *   - reads "input.txt"      -> reads the request body String
 *   - writes "output.txt"    -> returns a String
 *
 * The assign() recursion, the course-id renumbering logic, and the output
 * format are all unchanged from the original.
 */
@Service
public class CourseAllocationService {

    // key - "new" course id; value - list of profs (in priority order)
    private TreeMap<Integer, List<Integer>> master;

    // key - prof id; value - list of course ids currently allotted to that prof
    private TreeMap<Integer, List<Integer>> profAssignment;

    // set of profs who cannot be (re)assigned a particular course due to a clash
    private TreeSet<Integer> notAssignPid;

    // max courses (in units of 0.5) a prof can still take; key = prof id
    private TreeMap<Integer, Double> profPotential;

    private TreeSet<Integer> notAssignCid;

    private double getPotential(int profId) {
        return profPotential.computeIfAbsent(profId, k -> 0.0);
    }

    private List<Integer> assignmentsOf(int profId) {
        return profAssignment.computeIfAbsent(profId, k -> new ArrayList<>());
    }

    // ----------------------------------------------------------------
    // RECURSIVE SELF-CORRECTING ASSIGN FUNCTION (unchanged from original)
    // flag = 0 : 0.5 course to be given
    // flag = 1 : 1 full course to be given
    // ----------------------------------------------------------------
    private int assign(int cid, int flag) {
        int firsthalfProfId = -1;

        List<Integer> profsForCourse = master.get(cid);

        if (flag == 0) {
            for (int i = 0; i < profsForCourse.size(); i++) {
                int profId = profsForCourse.get(i);
                if (getPotential(profId) >= 0.5 && !notAssignPid.contains(profId)) {
                    profPotential.put(profId, getPotential(profId) - 0.5);
                    assignmentsOf(profId).add(cid);
                    return 1;
                }
            }
        }
        if (flag == 1) {
            for (int i = 0; i < profsForCourse.size(); i++) {
                int profId = profsForCourse.get(i);
                if (getPotential(profId) >= 0.5 && !notAssignPid.contains(profId)) {
                    profPotential.put(profId, getPotential(profId) - 0.5);
                    assignmentsOf(profId).add(cid);
                    flag = 0;
                    firsthalfProfId = profId;
                    break;
                }
            }
            if (flag == 0) {
                for (int i = 0; i < profsForCourse.size(); i++) {
                    int profId = profsForCourse.get(i);
                    if (getPotential(profId) >= 0.5 && !notAssignPid.contains(profId)) {
                        profPotential.put(profId, getPotential(profId) - 0.5);
                        assignmentsOf(profId).add(cid);
                        return 1;
                    }
                }
            }
        }

        if (flag == 0) {
            notAssignCid.add(cid);
            for (int i = 0; i < profsForCourse.size(); i++) {
                int profId = profsForCourse.get(i);

                if (!notAssignPid.contains(profId)) {
                    List<Integer> assigned = assignmentsOf(profId);
                    for (int j = 0; j < assigned.size(); j++) {
                        if (!notAssignCid.contains(assigned.get(j))) {
                            notAssignPid.add(profId);
                            int isAssigned = assign(assigned.get(j), 0);
                            notAssignPid.remove(profId);
                            if (isAssigned == 1) {
                                assigned.add(cid);
                                assigned.remove(j);
                                return 1;
                            } else {
                                notAssignCid.add(assigned.get(j));
                            }
                        }
                    }
                }
            }

            if (firsthalfProfId != -1) {
                List<Integer> lst = profAssignment.get(firsthalfProfId);
                lst.remove(lst.size() - 1);
            }
        }
        if (flag == 1) {
            double amountCourseAssigned = 0;

            notAssignCid.add(cid);
            outerFirstHalf:
            for (int i = 0; i < profsForCourse.size(); i++) {
                int profId = profsForCourse.get(i);

                if (!notAssignPid.contains(profId)) {
                    List<Integer> assigned = assignmentsOf(profId);
                    for (int j = 0; j < assigned.size(); j++) {
                        if (!notAssignCid.contains(assigned.get(j))) {
                            notAssignPid.add(profId);
                            int isAssigned = assign(assigned.get(j), 0);
                            notAssignPid.remove(profId);
                            if (isAssigned == 1) {
                                firsthalfProfId = profId;
                                assigned.add(cid);
                                assigned.remove(j);
                                amountCourseAssigned = 0.5;
                                break;
                            }
                        }
                    }
                    if (amountCourseAssigned == 0.5) break outerFirstHalf;
                }
            }
            notAssignCid.remove(cid);
            if (amountCourseAssigned == 0) {
                return -1;
            } else {
                notAssignCid.add(cid);
                outerSecondHalf:
                for (int i = 0; i < profsForCourse.size(); i++) {
                    int profId = profsForCourse.get(i);

                    if (!notAssignPid.contains(profId)) {
                        List<Integer> assigned = assignmentsOf(profId);
                        for (int j = 0; j < assigned.size(); j++) {
                            if (!notAssignCid.contains(assigned.get(j))) {
                                notAssignPid.add(profId);
                                int isAssigned = assign(assigned.get(j), 0);
                                notAssignPid.remove(profId);
                                if (isAssigned == 1) {
                                    assigned.add(cid);
                                    assigned.remove(j);
                                    amountCourseAssigned = 1;
                                    break;
                                }
                            }
                        }
                        if (amountCourseAssigned == 1) break outerSecondHalf;
                    }
                }
                notAssignCid.remove(cid);

                if (amountCourseAssigned == 1) {
                    return 1;
                } else {
                    List<Integer> lst = profAssignment.get(firsthalfProfId);
                    lst.remove(lst.size() - 1);
                }
            }
        }
        return -1;
    }

    /**
     * Runs the allocation algorithm against the given input text (same format
     * as the original input.txt) and returns the output text (same format as
     * the original output.txt).
     */
    public String runAllocation(String inputText) {

        // reset all state so this bean is safe to call again (e.g. from a
        // second, concurrent HTTP request)
        master = new TreeMap<>();
        profAssignment = new TreeMap<>();
        notAssignPid = new TreeSet<>();
        profPotential = new TreeMap<>();
        notAssignCid = new TreeSet<>();

        try {
            return runAlgorithm(inputText);
        } catch (IOException e) {
            // shouldn't happen for a StringReader, but keep the contract honest
            throw new AllocationException("Failed to process input: " + e.getMessage());
        } catch (RuntimeException e) {
            // malformed input (bad numbers, missing fields, etc.)
            throw new AllocationException("Malformed input: " + e.getMessage());
        }
    }

    private String runAlgorithm(String inputText) throws IOException {

        // ============================================================
        // PART 1 : PARSE INPUT (from String instead of input.txt)
        // ============================================================

        BufferedReader reader = new BufferedReader(new StringReader(inputText));

        String firstLine = reader.readLine();
        if (firstLine == null || firstLine.trim().isEmpty()) {
            throw new AllocationException("Input is empty or missing the professor count line.");
        }
        int numOfProfs = Integer.parseInt(firstLine.trim());

        TreeMap<Integer, String> profId = new TreeMap<>();
        TreeMap<String, Integer> revProfId = new TreeMap<>();
        TreeMap<Integer, String> courseId = new TreeMap<>();
        TreeMap<String, Integer> revCourseId = new TreeMap<>();
        TreeSet<String> courses = new TreeSet<>();

        TreeMap<Integer, List<Integer>> profFdCdc = new TreeMap<>();
        TreeMap<Integer, List<Integer>> profHdCdc = new TreeMap<>();
        TreeMap<Integer, List<Integer>> profFdElec = new TreeMap<>();
        TreeMap<Integer, List<Integer>> profHdElec = new TreeMap<>();

        List<List<String>> input = new ArrayList<>();

        for (int p = 0; p < numOfProfs; p++) {
            String line = reader.readLine();
            if (line == null) line = "";

            List<String> profDeets = new ArrayList<>(Arrays.asList(line.split(",", -1)));
            for (int t = 0; t < profDeets.size(); t++) {
                profDeets.set(t, profDeets.get(t).trim());
            }

            input.add(profDeets);

            profId.put(p + 1, profDeets.get(0));
            revProfId.put(profDeets.get(0), p + 1);

            profPotential.put(p + 1, Double.parseDouble(profDeets.get(1)));

            for (int k = 2; k < profDeets.size(); k++) {
                int numOfGivenTypeCourse = Integer.parseInt(profDeets.get(k));
                while (numOfGivenTypeCourse != 0) {
                    k++;
                    courses.add(profDeets.get(k));
                    numOfGivenTypeCourse--;
                }
            }
        }

        int cIdCounter = 1;
        for (String s : courses) {
            courseId.put(cIdCounter, s);
            revCourseId.put(s, cIdCounter);
            cIdCounter++;
        }

        for (int i = 0; i < input.size(); i++) {
            List<String> row = input.get(i);
            int flag = 0;
            for (int j = 2; j < row.size(); j++) {
                flag++;
                int numOfGivenTypeCourse = Integer.parseInt(row.get(j));
                while (numOfGivenTypeCourse != 0) {
                    j++;
                    int profKey = revProfId.get(row.get(0));
                    int courseKey = revCourseId.get(row.get(j));

                    if (flag == 1) {
                        profFdCdc.computeIfAbsent(profKey, k -> new ArrayList<>()).add(courseKey);
                    } else if (flag == 2) {
                        profHdCdc.computeIfAbsent(profKey, k -> new ArrayList<>()).add(courseKey);
                    } else if (flag == 3) {
                        profFdElec.computeIfAbsent(profKey, k -> new ArrayList<>()).add(courseKey);
                    } else if (flag == 4) {
                        profHdElec.computeIfAbsent(profKey, k -> new ArrayList<>()).add(courseKey);
                    }
                    numOfGivenTypeCourse--;
                }
            }
        }

        // key = old course id, value = revised ("new") course id
        // order of new id -> fdcdc, hdcdc, fdelec, hdelec
        TreeMap<Integer, Integer> newCid = new TreeMap<>();
        TreeMap<Integer, Integer> revNewCid = new TreeMap<>();
        int newidAllocator = 1;
        int columnNum;
        int count;

        columnNum = 0;
        count = 0;
        while (count < profFdCdc.size()) {
            count = 0;
            for (Map.Entry<Integer, List<Integer>> it : profFdCdc.entrySet()) {
                if (it.getValue().size() <= columnNum) {
                    count++;
                } else {
                    int oldId = it.getValue().get(columnNum);
                    if (newCid.getOrDefault(oldId, 0) == 0) {
                        newCid.put(oldId, newidAllocator);
                        revNewCid.put(newidAllocator, oldId);
                        newidAllocator++;
                    }
                }
            }
            columnNum++;
        }
        columnNum = 0;
        count = 0;
        while (count < profHdCdc.size()) {
            count = 0;
            for (Map.Entry<Integer, List<Integer>> it : profHdCdc.entrySet()) {
                if (it.getValue().size() <= columnNum) {
                    count++;
                } else {
                    int oldId = it.getValue().get(columnNum);
                    if (newCid.getOrDefault(oldId, 0) == 0) {
                        newCid.put(oldId, newidAllocator);
                        revNewCid.put(newidAllocator, oldId);
                        newidAllocator++;
                    }
                }
            }
            columnNum++;
        }
        columnNum = 0;
        count = 0;
        while (count < profFdElec.size()) {
            count = 0;
            for (Map.Entry<Integer, List<Integer>> it : profFdElec.entrySet()) {
                if (it.getValue().size() <= columnNum) {
                    count++;
                } else {
                    int oldId = it.getValue().get(columnNum);
                    if (newCid.getOrDefault(oldId, 0) == 0) {
                        newCid.put(oldId, newidAllocator);
                        revNewCid.put(newidAllocator, oldId);
                        newidAllocator++;
                    }
                }
            }
            columnNum++;
        }
        columnNum = 0;
        count = 0;
        while (count < profHdElec.size()) {
            count = 0;
            for (Map.Entry<Integer, List<Integer>> it : profHdElec.entrySet()) {
                if (it.getValue().size() <= columnNum) {
                    count++;
                } else {
                    int oldId = it.getValue().get(columnNum);
                    if (newCid.getOrDefault(oldId, 0) == 0) {
                        newCid.put(oldId, newidAllocator);
                        revNewCid.put(newidAllocator, oldId);
                        newidAllocator++;
                    }
                }
            }
            columnNum++;
        }

        // ============================================================
        // PART 2 : MAIN ALGORITHM
        // ============================================================

        columnNum = 0;
        count = 0;
        while (count < profFdCdc.size()) {
            count = 0;
            for (Map.Entry<Integer, List<Integer>> it : profFdCdc.entrySet()) {
                if (it.getValue().size() <= columnNum) {
                    count++;
                } else {
                    int oldId = it.getValue().get(columnNum);
                    master.computeIfAbsent(newCid.get(oldId), k -> new ArrayList<>()).add(it.getKey());
                }
            }
            columnNum++;
        }
        columnNum = 0;
        count = 0;
        while (count < profHdCdc.size()) {
            count = 0;
            for (Map.Entry<Integer, List<Integer>> it : profHdCdc.entrySet()) {
                if (it.getValue().size() <= columnNum) {
                    count++;
                } else {
                    int oldId = it.getValue().get(columnNum);
                    master.computeIfAbsent(newCid.get(oldId), k -> new ArrayList<>()).add(it.getKey());
                }
            }
            columnNum++;
        }
        columnNum = 0;
        count = 0;
        while (count < profFdElec.size()) {
            count = 0;
            for (Map.Entry<Integer, List<Integer>> it : profFdElec.entrySet()) {
                if (it.getValue().size() <= columnNum) {
                    count++;
                } else {
                    int oldId = it.getValue().get(columnNum);
                    master.computeIfAbsent(newCid.get(oldId), k -> new ArrayList<>()).add(it.getKey());
                }
            }
            columnNum++;
        }
        columnNum = 0;
        count = 0;
        while (count < profHdElec.size()) {
            count = 0;
            for (Map.Entry<Integer, List<Integer>> it : profHdElec.entrySet()) {
                if (it.getValue().size() <= columnNum) {
                    count++;
                } else {
                    int oldId = it.getValue().get(columnNum);
                    master.computeIfAbsent(newCid.get(oldId), k -> new ArrayList<>()).add(it.getKey());
                }
            }
            columnNum++;
        }

        // ============================================================
        // PART 3 : RUN THE ASSIGNMENT
        // ============================================================

        for (Map.Entry<Integer, List<Integer>> pr : master.entrySet()) {
            notAssignCid.clear();
            assign(pr.getKey(), 1);
        }

        // ============================================================
        // PART 4 : BUILD OUTPUT (into a String instead of output.txt)
        // ============================================================

        StringBuilder out = new StringBuilder();

        out.append("Courses:     Professors in order of priority:\n");
        for (Map.Entry<Integer, List<Integer>> pr : master.entrySet()) {
            out.append(courseId.get(revNewCid.get(pr.getKey()))).append("           ");
            for (int i = 0; i < pr.getValue().size(); i++) {
                out.append(profId.get(pr.getValue().get(i))).append(" ");
            }
            out.append("\n");
        }
        out.append("\n");

        for (Map.Entry<Integer, List<Integer>> pr : profAssignment.entrySet()) {
            TreeMap<Integer, Integer> assigned = new TreeMap<>();
            for (int i = 0; i < pr.getValue().size(); i++) {
                int cid = pr.getValue().get(i);
                assigned.merge(cid, 1, Integer::sum);
            }

            out.append(profId.get(pr.getKey())).append(" is assigned the following:\n");
            int i = 1;
            for (Map.Entry<Integer, Integer> it : assigned.entrySet()) {
                if (it.getValue() == 1) {
                    out.append("\t").append(i).append(". Half a course of ")
                       .append(courseId.get(revNewCid.get(it.getKey()))).append("\n");
                } else {
                    out.append("\t").append(i).append(". Full course of ")
                       .append(courseId.get(revNewCid.get(it.getKey()))).append("\n");
                }
                i++;
            }
            out.append("\n");
        }

        return out.toString();
    }
}
