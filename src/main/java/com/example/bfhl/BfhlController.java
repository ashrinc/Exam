package com.example.bfhl;

import com.example.bfhl.BfhlRequest;
import com.example.bfhl.BfhlResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping
public class BfhlController {

    private static final Pattern NUM = Pattern.compile("^-?\\d+$");
    private static final Pattern ALPHA = Pattern.compile("^[A-Za-z]+$");

    @Value("${app.full-name:John Doe}")
    private String fullName; // e.g., "John Doe"
    @Value("${app.dob-ddmmyyyy:17091999}")
    private String dob;      // e.g., "17091999"
    @Value("${app.email:john@xyz.com}")
    private String email;
    @Value("${app.roll-number:ABCD123}")
    private String rollNumber;

    @PostMapping("/bfhl")
    public ResponseEntity<BfhlResponse> bfhl(@RequestBody(required = false) BfhlRequest req) {
        BfhlResponse res = new BfhlResponse();
        try {
            List<String> data = (req != null && req.getData() != null) ? req.getData() : Collections.emptyList();

            List<String> even = new ArrayList<>();
            List<String> odd = new ArrayList<>();
            List<String> alphaUpper = new ArrayList<>();
            List<String> special = new ArrayList<>();
            BigInteger sum = BigInteger.ZERO;

            // For concat_string: gather ALL alphabetic characters in original input order
            StringBuilder alphaChars = new StringBuilder();

            for (String s : data) {
                if (s == null) { special.add(null); continue; }
                if (NUM.matcher(s).matches()) {
                    // number
                    try {
                        BigInteger n = new BigInteger(s);
                        sum = sum.add(n);
                        if (n.mod(BigInteger.TWO).equals(BigInteger.ZERO)) {
                            even.add(s);
                        } else {
                            odd.add(s);
                        }
                    } catch (Exception e) {
                        // should not happen due to regex, but be safe
                        special.add(s);
                    }
                } else if (ALPHA.matcher(s).matches()) {
                    // alphabets only
                    alphaUpper.add(s.toUpperCase(Locale.ROOT));
                    alphaChars.append(s); // keep original letters for concat process
                } else {
                    // everything else
                    special.add(s);
                }
            }

            String concat = alternatingCapsReverse(alphaChars.toString());

            String userId = buildUserId(fullName, dob);

            res.setIs_success(true);
            res.setUser_id(userId);
            res.setEmail(email);
            res.setRoll_number(rollNumber);
            res.setOdd_numbers(odd);
            res.setEven_numbers(even);
            res.setAlphabets(alphaUpper);
            res.setSpecial_characters(special);
            res.setSum(sum.toString());
            res.setConcat_string(concat);

            return ResponseEntity.ok(res);
        } catch (Exception ex) {
            // Graceful failure
            res.setIs_success(false);
            res.setUser_id(buildUserId(fullName, dob));
            res.setEmail(email);
            res.setRoll_number(rollNumber);
            res.setOdd_numbers(Collections.emptyList());
            res.setEven_numbers(Collections.emptyList());
            res.setAlphabets(Collections.emptyList());
            res.setSpecial_characters(Collections.emptyList());
            res.setSum("0");
            res.setConcat_string("");
            return ResponseEntity.status(HttpStatus.OK).body(res);
        }
    }

    private static String buildUserId(String fullName, String dobDdMmYyyy) {
        // full name must be lowercase and spaces to underscores
        String base = (fullName == null ? "" : fullName).trim().toLowerCase(Locale.ROOT);
        base = Arrays.stream(base.split("\\s+"))
                     .filter(StringUtils::hasText)
                     .collect(Collectors.joining("_"));
        return base + "_" + (dobDdMmYyyy == null ? "" : dobDdMmYyyy.trim());
    }

    /**
     * Reverse the sequence of letters, then produce alternating caps:
     * index 0 -> UPPER, index 1 -> lower, index 2 -> UPPER, ...
     */
    private static String alternatingCapsReverse(String letters) {
        if (letters == null || letters.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(letters.length());
        for (int i = letters.length() - 1, outIdx = 0; i >= 0; i--, outIdx++) {
            char ch = letters.charAt(i);
            if (Character.isLetter(ch)) {
                if (outIdx % 2 == 0) {
                    sb.append(Character.toUpperCase(ch));
                } else {
                    sb.append(Character.toLowerCase(ch));
                }
            }
        }
        return sb.toString();
    }
}
