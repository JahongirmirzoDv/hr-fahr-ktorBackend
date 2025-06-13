package com.fahr.hrplatform.routes

import com.fahr.hrplatform.models.ProjectDTO
import com.fahr.hrplatform.models.ProjectAssignmentDTO
import com.fahr.hrplatform.repository.EmployeeRepository
import com.fahr.hrplatform.repository.ProjectAssignmentRepository
import com.fahr.hrplatform.repository.ProjectRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.projectRoutes() {
    authenticate("auth-jwt") {
        route("/projects") {
            post {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString() ?: ""

                if (role != "admin" && role != "manager") {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admins and managers can create projects"))
                    return@post
                }

                val projectDTO = call.receive<ProjectDTO>()
                val projectRepository = ProjectRepository()

                val project = projectRepository.create(
                    name = projectDTO.name,
                    description = projectDTO.description,
                    startDate = projectDTO.startDate,
                    endDate = projectDTO.endDate,
                    status = projectDTO.status,
                    budget = projectDTO.budget
                )

                call.respond(HttpStatusCode.Created, project)
            }

            get {
                val status = call.request.queryParameters["status"]

                val projectRepository = ProjectRepository()
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

                val projectRepository = ProjectRepository()
                val project = projectRepository.findById(id) ?: run {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Project not found"))
                    return@get
                }

                call.respond(project)
            }

            // Project assignments endpoints
            post("/{id}/assignments") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString() ?: ""

                if (role != "admin" && role != "manager") {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admins and managers can assign employees to projects"))
                    return@post
                }

                val projectId = call.parameters["id"]
                if (projectId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing project id parameter"))
                    return@post
                }

                // Verify project exists
                val projectRepository = ProjectRepository()
                val project = projectRepository.findById(projectId) ?: run {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Project not found"))
                    return@post
                }

                val assignmentDTO = call.receive<ProjectAssignmentDTO>()

                // Verify employee exists
                val employeeRepository = EmployeeRepository()
                val employee = employeeRepository.findById(assignmentDTO.employeeId) ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Employee not found"))
                    return@post
                }

                val projectAssignmentRepository = ProjectAssignmentRepository()

                val assignment = projectAssignmentRepository.create(
                    projectId = projectId,
                    employeeId = assignmentDTO.employeeId,
                    role = assignmentDTO.role,
                    assignmentDate = assignmentDTO.assignmentDate ?: java.time.LocalDate.now(),
                    endDate = assignmentDTO.endDate,
                    isActive = assignmentDTO.isActive
                )

                call.respond(HttpStatusCode.Created, assignment)
            }

            get("/{id}/assignments") {
                val projectId = call.parameters["id"]
                if (projectId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing project id parameter"))
                    return@get
                }

                // Verify project exists
                val projectRepository = ProjectRepository()
                val project = projectRepository.findById(projectId) ?: run {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Project not found"))
                    return@get
                }

                val projectAssignmentRepository = ProjectAssignmentRepository()
                val assignments = projectAssignmentRepository.findByProject(projectId)

                call.respond(assignments)
            }
        }
    }
}
