let token = localStorage.getItem("token") || null;
let role = null;

let debounceTimeout;

let currentPage = 0;
let pageSize = 5;
let totalPages = 0;
let searchText = "";
let filterStatus = "";
let sortField = "id";
let sortDirection = "asc";

if (token) {
    const payload = parseJwt(token);
    role = payload?.role;
}

function parseJwt(token){
    try {
        return JSON.parse(atob(token.split(".")[1]));
    } catch (e) {
        return null;
    }   
}

// Login
async function login() {

    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;

    const response = await fetch("https://task-manager-java-zrc8.onrender.com/api/auth/login", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({username, password})
    });

    if(!response.ok){
        localStorage.removeItem("token");
        token = null;
        role = null;
        list.innerHTML = "";
        alert("Login failed");
        return;
    }

    const data = await response.json();
    
    token = data.token;
    localStorage.setItem("token",token);

    //extragem rolul din token
    const payload = parseJwt(token);
    role = payload.role;

    alert("Login successful");

    currentPage = 0;
    loadTasks();
}

// LOAD TASKS(PAGINATION)
async function loadTasks() {
    
    setLoading(true);

    try {

        if (!token) {
            alert("Please login first");
            return;
        }

        const list = document.getElementById("tasks");
        if (!list){
            console.error("tasks element not found");
            return;
        }

        list.innerHTML = "";

        for (let i = 0; i < 5; i++) {
            const sk = document.createElement("div");
            sk.className = "skeleton";
            list.appendChild(sk);
        }

        const query = new URLSearchParams({
            page: currentPage,
            size: pageSize,
            text: searchText,
            status: filterStatus,
            sort: sortField,
            direction: sortDirection
        });
        
        const url = role === "ADMIN"
            ? `https://task-manager-java-zrc8.onrender.com/api/admin/all-tasks?${query.toString()}`
            : `https://task-manager-java-zrc8.onrender.com/api/tasks?${query.toString()}`;

        const response = await fetch(url, { 
            headers: {
                "Authorization": "Bearer " + token
            }
        });

        if(!response.ok){
            list.innerHTML = "Error loading tasks";
            return;
        }

        const data = await response.json();
        const tasks = data.content;
        totalPages = data.totalPages;

        list.innerHTML = ""; 

        tasks.forEach(t => {
            const div = document.createElement("div");
            div.className = "task";

            div.innerHTML = `
                <span>
                <b>${t.title}</b><br> 
                <small>${t.status}</small>
                </span>
                ${role === "ADMIN" ? `<button onclick="deleteTask(${t.id})">Delete</button>` : ""}  
            `;

            list.appendChild(div);
        });

        updatePaginationUI();

    } catch (e) {
        console.error(e);
        document.getElementById("tasks").innerHTML = "Error loading tasks";
    } finally {
        setLoading(false); 
    }
}

// Create task
async function createTask() {
    
    if (!token) {
        alert("Login first");
        return;
    }

    const title = document.getElementById("task-title").value;
    const deadlineRaw = document.getElementById("task-deadline").value;
    const deadline = deadlineRaw ? deadlineRaw.split("T")[0] : null;

    const response = await fetch("https://task-manager-java-zrc8.onrender.com/api/tasks", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Authorization": "Bearer " + token
        },
        body: JSON.stringify({
            title, 
            description: "default",
            priority: 3,
            deadline,
            status: "TODO",
            estimatedMinutes: 30
        })
    });

    if (!response.ok){
        alert("Failed to create task");
        return;
    }

    document.getElementById("task-title").value = "";
    document.getElementById("task-deadline").value = "";

    currentPage = 0; //reset la prima pagina
    loadTasks();
}

// Delete task
async function deleteTask(id){
    
    if (!token) {
        alert("Login first");
        return;
    }
    
    const response = await fetch(`https://task-manager-java-zrc8.onrender.com/api/tasks/${id}`, { 
        method: "DELETE",
        headers: {
            "Authorization": "Bearer " + token
        }
    });

    if(!response.ok){
        alert("Failed to delete task");
        return;
    }

    loadTasks();
}

// PAGINATION UI
function nextPage(){
    if (currentPage < totalPages - 1){
        currentPage++;
        loadTasks();
    }
}

function prevPage(){
    if (currentPage > 0){
        currentPage--;
        loadTasks();
    }
}

function updatePaginationUI(){
    const info = document.getElementById("page-info");
    if (info){
        info.innerText = `Page ${currentPage + 1} / ${totalPages}`;
    }   
}

function applyFilters(){
    searchText = document.getElementById("search").value;
    filterStatus = document.getElementById("status").value;
    sortField = document.getElementById("sort").value;
    sortDirection = document.getElementById("direction").value;
    
    currentPage = 0;// reset pagination
    loadTasks();
}

function debounceLoadTasks(){
    clearTimeout(debounceTimeout);
 
    debounceTimeout = setTimeout(() => {
        currentPage = 0; // reset pagination
        loadTasks();
    }, 400);// 400ms delay
}

function setLoading(isLoading) {
    const btn = document.getElementById("load-btn");
    const loading = document.getElementById("loading");

    if (btn) btn.disabled = isLoading;
    if (loading) loading.style.display = isLoading ? "block" : "none";
}

window.onload = () => {
    if (token) {
        loadTasks();
    }
};