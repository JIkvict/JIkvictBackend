package org.jikvict.jikvictbackend.controller

import org.jikvict.jikvictbackend.service.SolutionChecker
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/solution-checker")
class SolutionCheckerController(
    private val solutionChecker: SolutionChecker
) {

    @PostMapping("/check", consumes = ["multipart/form-data"])
    fun checkSolution(
        @RequestParam file: MultipartFile,
    ) {
        solutionChecker.executeCode(file, 10)
    }

}