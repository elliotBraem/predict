import { useState, useEffect } from 'react'
import { supabase } from '../lib/store'
import Head from 'next/head'
import styles from '../styles/Home.module.css'

export default function Home() {
  const [isLoading, setLoading] = useState(true)
  const [questions, setQuestions] = useState(null)

  useEffect(() => {
    console.log(process.env.SUPABASE_PUBLIC_KEY)
    console.log(process.env.SUPABASE_URL)
    getQuestions()
  }, [])

  async function getQuestions() {
    try {
      setLoading(true)
      let { data, error, status } = await supabase
        .from('questions')
        .select(`question, option1, option2`)
      if (error && status !== 406) {
        throw error
      }

      if (data) {
        setQuestions(data)
      }
    } catch (error) {
      alert(error.message)
    } finally {
      setLoading(false)
    }
  }

  async function createQuestion() {
    const { data, error } = await supabase
      .from('questions')
      .insert([
        { question: 'Which pill?', option1: 'blue', option2: 'red' }
      ])
  }

  const options = {
    schema: 'public',
    headers: { 'x-my-custom-header': 'whoa' },
    autoRefreshToken: true,
    persistSession: true,
    detectSessionInUrl: true
  }

  return (
    <div className={styles.container}>
      <Head>
        <title>Whoa</title>
        <meta name="description" content="A survey" />
        <link rel="icon" href="/favicon.ico" />
      </Head>

      <main className={styles.main}>
        <div>
          <div>
            <h1 className={styles.title}>
              Which pill?
            </h1>
          </div>
        </div>
        <div className={styles.wrapper}>
          <div className={styles.left}>
            Blue
          </div>
          <div className={styles.right}>
            Red
          </div>
        </div>
{/* 
        <button
          type="button"
          onClick={createQuestion}
        >
          Create Question
        </button>
        <div className={styles.grid}>
          {isLoading ? 'Loading...' :
            questions.map(question => (
              <li key={question.id}>{question.question}</li>
            ))
          }
        </div> */}
      </main >

      <footer className={styles.footer}>
      </footer>
    </div >
  )
}




