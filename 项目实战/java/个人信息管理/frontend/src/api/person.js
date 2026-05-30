import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 5000
})

export function pagePersons(current, size, keyword) {
  return api.get('/persons', { params: { current, size, keyword } })
}

export function addPerson(person) {
  return api.post('/persons', person)
}

export function updatePerson(id, person) {
  return api.put(`/persons/${id}`, person)
}

export function deletePerson(id) {
  return api.delete(`/persons/${id}`)
}
