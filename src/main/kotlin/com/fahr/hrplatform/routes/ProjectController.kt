package com.fahr.hrplatform.routes

import com.fahr.hrplatform.models.ProjectDTO
import com.fahr.hrplatform.models.ProjectAssignmentDTO
import com.fahr.hrplatform.models.Role
import com.fahr.hrplatform.models.UserPrincipal
import com.fahr.hrplatform.models.requireRole
import com.fahr.hrplatform.repository.EmployeeRepository
import com.fahr.hrplatform.repository.ProjectAssignmentRepository
import com.fahr.hrplatform.repository.ProjectRepository
import com.fahr.hrplatform.utils.DateUtil
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.DateTimePeriod
import org.koin.ktor.ext.inject

fun Route.projectRoutes() {
    val projectRepository: ProjectRepository by inject()
    val employeeRepository: EmployeeRepository by inject()
    val projectAssignmentRepository: ProjectAssignmentRepository by inject()

    authenticate("auth-jwt") {
        route("/projects") {
            post {
                val principal = call.principal<UserPrincipal>()

                if (principal == null || !principal.requireRole(Role.ADMIN, Role.MANAGER)) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Only admins and managers can create projects")
                    )
                    return@post
                }

                val projectDTO = call.receive<ProjectDTO>()

                val project = projectRepository.create(
                    name = projectDTO.name,
                    description = projectDTO.description,
                    startDate = projectDTO.startDate,
                    endDate = projectDTO.endDate,
                    status = projectDTO.status,
                    budget = projectDTO.budget,
                    managerId = projectDTO.managerId,
                    employeeIds = projectDTO.employeeIds,
                    location = projectDTO.location
                )

                call.respond(HttpStatusCode.Created, project.toString())
            }

            get {
                val status = call.request.queryParameters["status"]

                val projects = if (status != null) {
                    projectRepository.findByStatus(status)
                } else {
                    projectRepository.findAll()
                }

                call.respond(projects)
            }

            get("/{id}") {
                val id = call.parameters["id"]
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id parameter"))
                    return@get
                }

                val project = projectRepository.findById(id) ?: run {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Project not found"))
                    return@get
                }

                call.respond(project)
            }

            get("/employee/{employeeId}") {
                val employeeId = call.parameters["employeeId"]
                if (employeeId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing employeeId parameter"))
                    return@get
                }

                val assignments = projectAssignmentRepository.findByEmployee(employeeId)
                val projects = assignments.mapNotNull { projectRepository.findById(it.projectId) }

                call.respond(projects)
            }

            post("/{id}/assignments") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString() ?: ""

                if (role != "admin" && role != "manager") {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Only admins and managers can assign employees to projects")
                    )
                    return@post
                }

                val projectId = call.parameters["id"]
                if (projectId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing project id parameter"))
                    return@post
                }

                // Verify project exists
                val project = projectRepository.findById(projectId) ?: run {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Project not found"))
                    return@post
                }

                val assignmentDTO = call.receive<ProjectAssignmentDTO>()

                // Verify employee exists
                val employee = employeeRepository.findById(assignmentDTO.employeeId) ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Employee not found"))
                    return@post
                }

                val assignment = projectAssignmentRepository.create(
                    projectId = projectId,
                    employeeId = assignmentDTO.employeeId,
                    role = assignmentDTO.role,
                    assignmentDate = assignmentDTO.assignmentDate ?: DateUtil.dateInUtc,
                    endDate = assignmentDTO.endDate,
                    isActive = assignmentDTO.isActive
                )

                call.respond(HttpStatusCode.Created, assignment.toString())
            }

            get("/{id}/assignments") {
                val projectId = call.parameters["id"]
                if (projectId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing project id parameter"))
                    return@get
                }

                // Verify project exists
                val project = projectRepository.findById(projectId) ?: run {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Project not found"))
                    return@get
                }

                val assignments = projectAssignmentRepository.findByProject(projectId)

                call.respond(assignments)
            }

            put ("/{id"){  }

            delete("/{id}"){}

        }
    }
}